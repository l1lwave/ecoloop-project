package com.ecoapp.scan.client;

import com.ecoapp.scan.client.dto.OpenFoodFactsResponse;
import com.ecoapp.scan.client.dto.OpenFoodFactsResponse.OFFProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

/**
 * Client for the Open Food Facts REST API v2.
 * No API key required.
 * Docs: https://openfoodfacts.github.io/openfoodfacts-server/api/
 */
@Component
public class OpenFoodFactsClient {

    private static final Logger log = LoggerFactory.getLogger(OpenFoodFactsClient.class);

    /**
     * v2 API with field projection — much faster than v0 (avoids 50+ KB responses).
     * We request only the fields we actually use.
     */
    private static final String API_URL =
            "https://world.openfoodfacts.org/api/v2/product/{barcode}" +
            "?fields=status,product_name,product_name_nl,product_name_en,product_name_uk," +
                    "brands,categories,categories_tags,packaging_text,packaging_materials_tags,quantity";

    private final RestTemplate restTemplate;

    public OpenFoodFactsClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<OFFProduct> lookup(String barcode) {
        try {
            log.info("OFF lookup: barcode={}", barcode);

            ResponseEntity<OpenFoodFactsResponse> response = restTemplate.exchange(
                    API_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(new HttpHeaders()),
                    OpenFoodFactsResponse.class,
                    barcode
            );

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                log.warn("OFF returned status {} for barcode {}", response.getStatusCode(), barcode);
                return Optional.empty();
            }

            OpenFoodFactsResponse body = response.getBody();
            if (!body.isFound()) {
                log.info("OFF: product not found for barcode {}", barcode);
                return Optional.empty();
            }

            OFFProduct product = body.getProduct();
            log.info("OFF: found '{}' [{}] for barcode {}",
                    product.getLocalizedName("nl"), product.getBrands(), barcode);
            return Optional.of(product);

        } catch (HttpClientErrorException.NotFound e) {
            log.info("OFF: 404 for barcode {}", barcode);
            return Optional.empty();
        } catch (Exception e) {
            log.error("OFF API error for barcode {}: {} — {}", barcode,
                    e.getClass().getSimpleName(), e.getMessage());
            return Optional.empty();
        }
    }
}
