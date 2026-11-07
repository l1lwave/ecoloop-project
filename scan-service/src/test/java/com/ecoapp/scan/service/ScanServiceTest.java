package com.ecoapp.scan.service;

import com.ecoapp.scan.client.OpenFoodFactsClient;
import com.ecoapp.scan.client.dto.OpenFoodFactsResponse.OFFProduct;
import com.ecoapp.scan.dto.ScanHistoryResponse;
import com.ecoapp.scan.dto.ScanResponse;
import com.ecoapp.scan.exception.ProductNotFoundException;
import com.ecoapp.scan.model.Material;
import com.ecoapp.scan.model.Product;
import com.ecoapp.scan.model.ScanEvent;
import com.ecoapp.scan.repository.MaterialRepository;
import com.ecoapp.scan.repository.ProductRepository;
import com.ecoapp.scan.repository.ScanEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScanServiceTest {

    private static final String BARCODE = "8718000000001";
    private static final double EPSILON = 0.0001;
    private static final String USER_ID = "user-1";
    private static final String OTHER_USER_ID = "user-2";
    private static final ZoneId AMS = ZoneId.of("Europe/Amsterdam");
    /** 09:00 UTC on 27 Oct 2026 is 10:00 in Amsterdam (CET, after the 25 Oct DST change). */
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-10-27T09:00:00Z"), AMS);

    @Mock ProductRepository productRepo;
    @Mock MaterialRepository materialRepo;
    @Mock OpenFoodFactsClient offClient;
    @Mock ScanEventRepository scanEventRepo;

    private ScanService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new ScanService(productRepo, materialRepo, offClient, scanEventRepo, FIXED_CLOCK);
        // No material rows in the database: the service falls back to its built-in material info.
        lenient().when(materialRepo.findById(anyString())).thenReturn(Optional.empty());
    }

    /** Builds an Open Food Facts product from JSON, using the same field names as the API. */
    private OFFProduct offProduct(String json) throws Exception {
        return mapper.readValue(json, OFFProduct.class);
    }

    private Product cachedProduct(String materialCode, double deposit, Boolean beverage) {
        Product p = new Product();
        p.setBarcode(BARCODE);
        p.setProductName("Cached product");
        p.setBrand("Brand");
        p.setMaterialCode(materialCode);
        p.setDepositValue(deposit);
        p.setBeverage(beverage);
        return p;
    }

    private void assertDeposit(double expected, ScanResponse response) {
        assertEquals(expected, response.depositValue(), EPSILON);
    }

    @Test
    void identify_peanutButterWithPetTextAndNoBeverageCategory_isNotDepositEligible() throws Exception {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.empty());
        when(offClient.lookup(BARCODE)).thenReturn(Optional.of(offProduct("""
                {"product_name":"Pindakaas","brands":"Calve",
                 "packaging_text":"PET-pot",
                 "categories_tags":["en:peanut-butters","en:spreads"]}
                """)));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.0, response);
        assertEquals("01 PET", response.materialType());
        assertTrue(response.recyclable());
    }

    @Test
    void identify_petBottleWithBeverageCategory_isDepositEligibleAt015() throws Exception {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.empty());
        when(offClient.lookup(BARCODE)).thenReturn(Optional.of(offProduct("""
                {"product_name":"Spa Blauw 1.5L","brands":"Spa","quantity":"1.5 l",
                 "packaging_materials_tags":["en:pet-1-polyethylene-terephthalate","en:plastic"],
                 "categories_tags":["en:beverages","en:waters"]}
                """)));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.15, response);
        assertEquals("01 PET", response.materialType());
    }

    @Test
    void identify_aluminiumCanWithBeverageCategory_isDepositEligibleAt015() throws Exception {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.empty());
        when(offClient.lookup(BARCODE)).thenReturn(Optional.of(offProduct("""
                {"product_name":"Heineken 330ml blik","brands":"Heineken",
                 "packaging_materials_tags":["en:aluminium"],
                 "categories_tags":["en:beers","en:alcoholic-beverages"]}
                """)));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.15, response);
        assertEquals("Alu", response.materialType());
    }

    @Test
    void identify_glassBottleWithBeverageCategory_isDepositEligibleAt015() throws Exception {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.empty());
        when(offClient.lookup(BARCODE)).thenReturn(Optional.of(offProduct("""
                {"product_name":"Fentimans Ginger Beer","brands":"Fentimans",
                 "packaging_materials_tags":["en:glass"],
                 "categories_tags":["en:beverages","en:sodas"]}
                """)));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.15, response);
        assertEquals("GL", response.materialType());
    }

    @Test
    void identify_unknownPackaging_isNotDepositEligibleAndNotDefaultedToPet() throws Exception {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.empty());
        when(offClient.lookup(BARCODE)).thenReturn(Optional.of(offProduct("""
                {"product_name":"Mystery Drink","brands":"Unknown",
                 "categories_tags":["en:beverages"]}
                """)));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.0, response);
        assertEquals("unknown", response.materialType());
        assertFalse(response.recyclable());
    }

    @Test
    void identify_competitionInText_doesNotMakeProductPet() throws Exception {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.empty());
        when(offClient.lookup(BARCODE)).thenReturn(Optional.of(offProduct("""
                {"product_name":"Cola Competition Edition","brands":"Cola",
                 "packaging_text":"Competitive price, competition edition",
                 "categories_tags":["en:beverages"]}
                """)));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertEquals("unknown", response.materialType());
        assertDeposit(0.0, response);
    }

    @Test
    void identify_cachedRowWithStaleDepositValue_isReturnedWithCorrectedValueAndSaved() {
        // Cached under old rules: a non-beverage was given 0.15.
        Product stale = cachedProduct("01 PET", 0.15, false);
        when(productRepo.findById(BARCODE)).thenReturn(Optional.of(stale));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.0, response);
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(saved.capture());
        assertEquals(0.0, saved.getValue().getDepositValue(), EPSILON);
    }

    @Test
    void identify_cachedBeverageRowWithZeroDeposit_isReturnedWithDepositValue() {
        Product stale = cachedProduct("01 PET", 0.0, true);
        when(productRepo.findById(BARCODE)).thenReturn(Optional.of(stale));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.15, response);
    }

    @Test
    void identify_cachedBeveragePetRow_returnsDepositWithoutCallingOpenFoodFacts() {
        // Seeded bottle: the beverage flag is set, so the cached row must be trusted as-is.
        when(productRepo.findById(BARCODE)).thenReturn(Optional.of(cachedProduct("01 PET", 0.15, true)));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.15, response);
        assertEquals("01 PET", response.materialType());
        verifyNoInteractions(offClient);
    }

    @Test
    void identify_cachedRowWithCorrectDeposit_doesNotWriteToDatabase() {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.of(cachedProduct("Alu", 0.15, true)));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.15, response);
        verify(productRepo, never()).save(any());
    }

    @Test
    void identify_legacyCachedRowWithoutBeverageFlag_isReclassifiedFromOpenFoodFacts() throws Exception {
        // Legacy row: PET by old default, but actually a peanut butter jar.
        when(productRepo.findById(BARCODE)).thenReturn(Optional.of(cachedProduct("01 PET", 0.15, null)));
        when(offClient.lookup(BARCODE)).thenReturn(Optional.of(offProduct("""
                {"product_name":"Pindakaas","brands":"Calve",
                 "packaging_text":"PET-pot",
                 "categories_tags":["en:peanut-butters"]}
                """)));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.0, response);
        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(productRepo).save(saved.capture());
        assertFalse(saved.getValue().getBeverage());
    }

    @Test
    void identify_legacyCachedRowNotVerifiable_returnsZeroDepositWithoutWriting() {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.of(cachedProduct("01 PET", 0.15, null)));
        when(offClient.lookup(BARCODE)).thenReturn(Optional.empty());

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertDeposit(0.0, response);
        verify(productRepo, never()).save(any());
    }

    // ── Scan event recording ──────────────────────────────────────────────────

    @Test
    void identify_successfulScan_recordsExactlyOneEventWithHeaderUserId() {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.of(cachedProduct("Alu", 0.15, true)));

        service.identify(BARCODE, "nl", USER_ID);

        ArgumentCaptor<ScanEvent> saved = ArgumentCaptor.forClass(ScanEvent.class);
        verify(scanEventRepo, times(1)).save(saved.capture());
        ScanEvent event = saved.getValue();
        assertEquals(USER_ID, event.getUserId());
        assertEquals(BARCODE, event.getBarcode());
        assertEquals("Cached product", event.getProductName());
        assertEquals("Alu", event.getMaterialCode());
        assertEquals(0.15, event.getDepositValue(), EPSILON);
        assertEquals(LocalDateTime.of(2026, 10, 27, 10, 0), event.getScannedAt());
    }

    @Test
    void identify_productNotFound_recordsNoEvent() {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.empty());
        when(offClient.lookup(BARCODE)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> service.identify(BARCODE, "nl", USER_ID));

        verify(scanEventRepo, never()).save(any());
    }

    @Test
    void identify_externalLookupFails_recordsNoEvent() {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.empty());
        when(offClient.lookup(BARCODE)).thenThrow(new RuntimeException("upstream unavailable"));

        assertThrows(RuntimeException.class, () -> service.identify(BARCODE, "nl", USER_ID));

        verify(scanEventRepo, never()).save(any());
    }

    @Test
    void identify_eventWriteFails_stillReturnsIdentifyResult() {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.of(cachedProduct("Alu", 0.15, true)));
        when(scanEventRepo.save(any(ScanEvent.class)))
                .thenThrow(new DataAccessResourceFailureException("database unavailable"));

        ScanResponse response = service.identify(BARCODE, "nl", USER_ID);

        assertEquals(BARCODE, response.barcode());
        assertDeposit(0.15, response);
        verify(scanEventRepo).save(any(ScanEvent.class));
    }

    // ── Scan history ──────────────────────────────────────────────────────────

    private ScanEvent event(String userId, LocalDateTime scannedAt, String materialCode) {
        ScanEvent e = new ScanEvent();
        e.setUserId(userId);
        e.setBarcode(BARCODE);
        e.setProductName("Product at " + scannedAt);
        e.setMaterialCode(materialCode);
        e.setDepositValue(0.15);
        e.setScannedAt(scannedAt);
        return e;
    }

    @Test
    void history_returnsRowsNewestFirstWithMaterialLabel() {
        ScanEvent newer = event(USER_ID, LocalDateTime.of(2026, 10, 27, 10, 0), "01 PET");
        ScanEvent older = event(USER_ID, LocalDateTime.of(2026, 10, 26, 9, 0), "01 PET");
        Material pet = new Material();
        pet.setCode("01 PET");
        pet.setLabel("PET");
        pet.setRecyclable(true);
        when(materialRepo.findById("01 PET")).thenReturn(Optional.of(pet));
        when(scanEventRepo.findByUserIdOrderByScannedAtDescIdDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(newer, older), PageRequest.of(0, 20), false));

        ScanHistoryResponse response = service.history(USER_ID, 0, 20);

        assertEquals(2, response.items().size());
        assertEquals(LocalDateTime.of(2026, 10, 27, 10, 0), response.items().get(0).scannedAt());
        assertEquals(LocalDateTime.of(2026, 10, 26, 9, 0), response.items().get(1).scannedAt());
        assertEquals("PET", response.items().get(0).materialLabel());
        assertTrue(response.items().get(0).recyclable());
        assertEquals(0.15, response.items().get(0).depositValue(), EPSILON);
        assertEquals(0, response.page());
        assertEquals(20, response.size());
    }

    @Test
    void history_sizeAboveFifty_isCappedAtFifty() {
        when(scanEventRepo.findByUserIdOrderByScannedAtDescIdDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 50), false));

        ScanHistoryResponse response = service.history(USER_ID, 0, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(scanEventRepo).findByUserIdOrderByScannedAtDescIdDesc(eq(USER_ID), captor.capture());
        assertEquals(50, captor.getValue().getPageSize());
        assertEquals(50, response.size());
    }

    @Test
    void history_lastPage_hasMoreIsFalse() {
        when(scanEventRepo.findByUserIdOrderByScannedAtDescIdDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(2, 20), false));

        ScanHistoryResponse response = service.history(USER_ID, 2, 20);

        assertFalse(response.hasMore());
        assertEquals(2, response.page());
    }

    @Test
    void history_pageWithMoreRows_hasMoreIsTrue() {
        when(scanEventRepo.findByUserIdOrderByScannedAtDescIdDesc(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 20), true));

        assertTrue(service.history(USER_ID, 0, 20).hasMore());
    }

    @Test
    void history_queriesOnlyTheCallersRows() {
        when(scanEventRepo.findByUserIdOrderByScannedAtDescIdDesc(eq(OTHER_USER_ID), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 20), false));

        service.history(OTHER_USER_ID, 0, 20);

        verify(scanEventRepo).findByUserIdOrderByScannedAtDescIdDesc(eq(OTHER_USER_ID), any(Pageable.class));
        verifyNoMoreInteractions(scanEventRepo);
    }

    @Test
    void getAllMaterials_returnsAllRowsFromRepository() {
        List<Material> materials = List.of(new Material());
        when(materialRepo.findAll()).thenReturn(materials);

        assertEquals(materials, service.getAllMaterials());
    }

    @Test
    void checkDeposit_unknownBarcodeIsNotEligibleAndDoesNotCallOpenFoodFacts() {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.empty());

        var r = service.checkDeposit(BARCODE);

        assertFalse(r.eligible());
        assertEquals(0.0, r.depositValue(), EPSILON);
        verifyNoInteractions(offClient);
    }

    @Test
    void checkDeposit_productWithDepositIsEligible() {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.of(cachedProduct("01 PET", 0.15, true)));

        var r = service.checkDeposit(BARCODE);

        assertTrue(r.eligible());
        assertEquals(0.15, r.depositValue(), EPSILON);
        verifyNoInteractions(offClient);
    }

    @Test
    void checkDeposit_productWithoutDepositIsNotEligible() {
        when(productRepo.findById(BARCODE)).thenReturn(Optional.of(cachedProduct("unknown", 0.0, false)));

        var r = service.checkDeposit(BARCODE);

        assertFalse(r.eligible());
        assertEquals(0.0, r.depositValue(), EPSILON);
    }
}
