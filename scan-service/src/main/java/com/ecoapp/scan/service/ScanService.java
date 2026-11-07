package com.ecoapp.scan.service;

import com.ecoapp.scan.client.OpenFoodFactsClient;
import com.ecoapp.scan.client.dto.OpenFoodFactsResponse.OFFProduct;
import com.ecoapp.scan.dto.DepositCheckResponse;
import com.ecoapp.scan.dto.ScanHistoryItem;
import com.ecoapp.scan.dto.ScanHistoryResponse;
import com.ecoapp.scan.dto.ScanResponse;
import com.ecoapp.scan.exception.ProductNotFoundException;
import com.ecoapp.scan.model.Material;
import com.ecoapp.scan.model.Product;
import com.ecoapp.scan.model.ScanEvent;
import com.ecoapp.scan.repository.MaterialRepository;
import com.ecoapp.scan.repository.ProductRepository;
import com.ecoapp.scan.repository.ScanEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    /** Upper bound for one page of scan history, regardless of the requested size. */
    static final int MAX_HISTORY_PAGE_SIZE = 50;

    /** Material codes as stored in the materials table (see data.sql). */
    static final String PET_CODE   = "01 PET";
    static final String ALU_CODE   = "Alu";
    static final String GLASS_CODE = "GL";
    static final String HDPE_CODE  = "02 HDPE";
    static final String LDPE_CODE  = "04 LDPE";
    static final String PP_CODE    = "05 PP";
    static final String FE_CODE    = "Fe";

    /** Packaging could not be determined. Never defaulted to PET. */
    static final String UNKNOWN_CODE = "unknown";

    static final double DEPOSIT_VALUE = 0.15;

    /** Category tag segments that mark a product as a beverage (whole segments only). */
    private static final Set<String> BEVERAGE_TERMS = Set.of(
            "beverage", "beverages", "water", "waters", "soda", "sodas",
            "juice", "juices", "beer", "beers", "drink", "drinks");

    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;
    private final OpenFoodFactsClient offClient;
    private final ScanEventRepository scanEventRepository;
    private final Clock clock;

    public ScanService(ProductRepository productRepository,
                       MaterialRepository materialRepository,
                       OpenFoodFactsClient offClient,
                       ScanEventRepository scanEventRepository,
                       Clock clock) {
        this.productRepository = productRepository;
        this.materialRepository = materialRepository;
        this.offClient = offClient;
        this.scanEventRepository = scanEventRepository;
        this.clock = clock;
    }

    /**
     * Lookup order:
     * 1. Local DB  (seeded / previously cached)
     * 2. Open Food Facts API  → cache result
     * 3. Heuristic fallback (none: unknown packaging is not deposit-eligible)
     *
     * Cached deposit values are recomputed from the stored material code and beverage flag,
     * so rows cached under older rules are corrected on read.
     *
     * A scan event is recorded only when the lookup succeeds. The userId comes from the
     * gateway X-User-Id header via the controller.
     */
    public ScanResponse identify(String barcode, String lang, String userId) {
        ScanResponse response = productRepository.findById(barcode)
                .map(p -> p.getBeverage() == null
                        ? refreshLegacyProduct(p, lang)
                        : correctStoredDeposit(p, lang))
                .orElseGet(() -> lookupOffAndCache(barcode, lang));
        recordScanEvent(userId, response);
        return response;
    }

    public List<Material> getAllMaterials() {
        return materialRepository.findAll();
    }

    /**
     * Returns one page of the caller's scans, newest first.
     * Only rows owned by userId are queried. The page size is capped at MAX_HISTORY_PAGE_SIZE.
     */
    public ScanHistoryResponse history(String userId, int page, int size) {
        int effectiveSize = Math.min(size, MAX_HISTORY_PAGE_SIZE);
        Slice<ScanEvent> slice = scanEventRepository.findByUserIdOrderByScannedAtDescIdDesc(
                userId, PageRequest.of(page, effectiveSize));
        List<ScanHistoryItem> items = slice.getContent().stream()
                .map(this::toHistoryItem)
                .toList();
        return new ScanHistoryResponse(items, page, effectiveSize, slice.hasNext());
    }

    // ── OFF lookup ────────────────────────────────────────────────────────────

    private ScanResponse lookupOffAndCache(String barcode, String lang) {
        Optional<OFFProduct> external = offClient.lookup(barcode);
        if (external.isEmpty()) {
            throw new ProductNotFoundException(barcode);
        }

        Product cached = new Product();
        cached.setBarcode(barcode);
        applyClassification(cached, external.get(), lang);
        productRepository.save(cached);

        return toResponse(cached, lang);
    }

    /**
     * Re-classifies a row cached before the beverage flag existed.
     * If Open Food Facts no longer knows the product, the row is returned with deposit 0.0
     * and is not written back, so unverified data never pays out a deposit.
     */
    private ScanResponse refreshLegacyProduct(Product cached, String lang) {
        Optional<OFFProduct> external = offClient.lookup(cached.getBarcode());
        if (external.isEmpty()) {
            return toResponse(cached, lang);
        }
        applyClassification(cached, external.get(), lang);
        productRepository.save(cached);
        return toResponse(cached, lang);
    }

    /** Writes the classification of an OFF product onto the entity. */
    private void applyClassification(Product target, OFFProduct p, String lang) {
        String materialCode = inferMaterialCode(p);
        target.setProductName(p.getLocalizedName(lang));
        target.setBrand(p.getBrands());
        target.setMaterialCode(materialCode);
        target.setBeverage(isBeverage(p));
        target.setDepositValue(depositValue(target.getBeverage(), materialCode));
        target.setVolumeMl(parseVolumeMl(p.getQuantity()));
    }

    // ── Material and beverage classification ──────────────────────────────────

    /**
     * Returns the material code from packaging information only.
     * Packaging tags first, then category tags, then the packaging text.
     * Returns UNKNOWN_CODE when nothing matches.
     */
    private String inferMaterialCode(OFFProduct p) {
        // 1. packaging_materials_tags, e.g. "en:pet-1-polyethylene-terephthalate"
        if (p.getPackagingMaterialsTags() != null) {
            for (String tag : p.getPackagingMaterialsTags()) {
                String code = materialFromTerms(normalise(tag));
                if (code != null) return code;
            }
        }
        // 2. categories_tags, only packaging-specific phrases (e.g. "en:glass-bottles")
        if (p.getCategoriesTags() != null) {
            for (String tag : p.getCategoriesTags()) {
                String code = materialFromCategoryTerms(normalise(tag));
                if (code != null) return code;
            }
        }
        // 3. Free-text packaging description
        String code = materialFromTerms(normalise(p.getPackagingText()));
        return code != null ? code : UNKNOWN_CODE;
    }

    /** Matches whole words or phrases in an already normalised, space-padded text. */
    private String materialFromTerms(String padded) {
        if (hasTerm(padded, "pet", "polyethylene terephthalate", "plastic bottle", "plastic bottles", "plastic fles", "plastic flessen")) return PET_CODE;
        if (hasTerm(padded, "aluminium", "aluminum", "alu", "blik", "blikje")) return ALU_CODE;
        if (hasTerm(padded, "glass", "glas")) return GLASS_CODE;
        if (hasTerm(padded, "hdpe", "high density polyethylene")) return HDPE_CODE;
        if (hasTerm(padded, "ldpe", "low density polyethylene")) return LDPE_CODE;
        if (hasTerm(padded, "polypropylene", "pp")) return PP_CODE;
        if (hasTerm(padded, "steel", "tin")) return FE_CODE;
        return null;
    }

    /** Category tags use only packaging phrases, so food categories such as "pet food" do not match. */
    private String materialFromCategoryTerms(String padded) {
        if (hasTerm(padded, "beverage can", "beverage cans", "metal beverage can")) return ALU_CODE;
        if (hasTerm(padded, "glass bottle", "glass bottles")) return GLASS_CODE;
        if (hasTerm(padded, "pet bottle", "pet bottles", "plastic bottle", "plastic bottles")) return PET_CODE;
        return null;
    }

    /**
     * A product is a beverage only if one of its category tag segments is a beverage word.
     * Tags are split on ':' and '-', so "en:carbonated-drinks" yields the segment "drinks".
     */
    private boolean isBeverage(OFFProduct p) {
        if (p.getCategoriesTags() == null) return false;
        for (String tag : p.getCategoriesTags()) {
            if (tag == null) continue;
            for (String segment : tag.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
                if (BEVERAGE_TERMS.contains(segment)) return true;
            }
        }
        return false;
    }

    /** Deposit is paid only for beverage containers made of PET, aluminium or glass. */
    private static boolean isDepositEligible(boolean beverage, String materialCode) {
        if (!beverage) return false;
        return PET_CODE.equals(materialCode)
                || ALU_CODE.equals(materialCode)
                || GLASS_CODE.equals(materialCode);
    }

    private static double depositValue(Boolean beverage, String materialCode) {
        return isDepositEligible(Boolean.TRUE.equals(beverage), materialCode) ? DEPOSIT_VALUE : 0.0;
    }

    /** Lower-cases the text, replaces every non-letter/digit with a space, and pads both ends. */
    private static String normalise(String text) {
        if (text == null) return "  ";
        String cleaned = text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        return " " + cleaned + " ";
    }

    /** Whole-word or whole-phrase match on a normalised, space-padded text. */
    private static boolean hasTerm(String padded, String... terms) {
        for (String term : terms) {
            if (padded.contains(" " + term + " ")) return true;
        }
        return false;
    }

    // ── Scan events ───────────────────────────────────────────────────────────

    /**
     * Writes one scan event. A failed write must not hide a successful lookup,
     * so the failure is logged and the identify result is still returned.
     * The log line carries no barcode or user ID.
     */
    private void recordScanEvent(String userId, ScanResponse response) {
        ScanEvent event = new ScanEvent();
        event.setUserId(userId);
        event.setBarcode(response.barcode());
        event.setProductName(response.productName());
        event.setMaterialCode(response.materialType());
        event.setDepositValue(response.depositValue());
        event.setScannedAt(LocalDateTime.now(clock));
        try {
            scanEventRepository.save(event);
        } catch (RuntimeException e) {
            log.warn("Could not record scan event ({})", e.getClass().getSimpleName());
        }
    }

    private ScanHistoryItem toHistoryItem(ScanEvent event) {
        Material material = findMaterial(event.getMaterialCode());
        return new ScanHistoryItem(event.getBarcode(), event.getProductName(), material.getLabel(),
                material.isRecyclable(), event.getDepositValue(), event.getScannedAt());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Builds the response from the stored row, with the deposit recomputed from the current rules. */
    private ScanResponse toResponse(Product p, String lang) {
        double depositValue = depositValue(p.getBeverage(), p.getMaterialCode());
        Material material = findMaterial(p.getMaterialCode());
        return new ScanResponse(p.getBarcode(), p.getProductName(), p.getBrand(),
                p.getMaterialCode(), material.getLabel(), material.isRecyclable(),
                depositValue, localizedInstructions(material, lang));
    }

    /** Corrects a stored deposit value that no longer matches the current rules, and persists the fix. */
    private ScanResponse correctStoredDeposit(Product p, String lang) {
        double expected = depositValue(p.getBeverage(), p.getMaterialCode());
        if (Double.compare(p.getDepositValue(), expected) != 0) {
            p.setDepositValue(expected);
            productRepository.save(p);
        }
        return toResponse(p, lang);
    }

    private Material findMaterial(String code) {
        return materialRepository.findById(code)
                .orElseGet(() -> fallbackMaterial(code));
    }

    /**
     * Returns the instructions in the requested language.
     * Falls back gracefully if a translation column is null.
     */
    private String localizedInstructions(Material m, String lang) {
        return switch (lang) {
            case "en" -> m.getInstructionsEn() != null ? m.getInstructionsEn() : m.getInstructions();
            case "ua" -> m.getInstructionsUa() != null ? m.getInstructionsUa() : m.getInstructions();
            default   -> m.getInstructions();
        };
    }

    private Material fallbackMaterial(String code) {
        if (UNKNOWN_CODE.equals(code)) {
            return unknownPackagingMaterial();
        }
        Material m = new Material();
        m.setCode(code);
        m.setLabel(code);
        m.setRecyclable(true);
        m.setInstructions("Lever in bij een statiegeldpunt.");
        m.setInstructionsEn("Return at a deposit point.");
        m.setInstructionsUa("Здайте у пункті прийому.");
        return m;
    }

    /** Packaging is unknown: not recyclable as a known material and not returnable under statiegeld. */
    private Material unknownPackagingMaterial() {
        Material m = new Material();
        m.setCode(UNKNOWN_CODE);
        m.setLabel("Onbekend");
        m.setRecyclable(false);
        m.setInstructions("Niet in aanmerking voor statiegeld. Controleer de lokale regels voor afvalscheiding.");
        m.setInstructionsEn("Not returnable under the deposit scheme. Check local waste sorting rules.");
        m.setInstructionsUa("Не підлягає поверненню за системою застави. Перевірте місцеві правила сортування.");
        return m;
    }

    private Integer parseVolumeMl(String quantity) {
        if (quantity == null || quantity.isBlank()) return null;
        try {
            String lower  = quantity.toLowerCase(Locale.ROOT);
            String digits = quantity.replaceAll("[^0-9.]", "");
            if (digits.isEmpty()) return null;
            double value = Double.parseDouble(digits);
            if (lower.contains(" l") && !lower.contains("ml")) value *= 1000;
            return (int) value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Deposit check for other services. Reads the products table only and never calls Open Food Facts,
     * so a barcode nobody has identified yet is not eligible. The caller has validated the barcode format.
     */
    public DepositCheckResponse checkDeposit(String barcode) {
        return productRepository.findById(barcode)
                .filter(p -> p.getDepositValue() > 0)
                .map(p -> new DepositCheckResponse(true, p.getDepositValue()))
                .orElseGet(() -> new DepositCheckResponse(false, 0.0));
    }
}
