package com.ecoapp.reward.client;

import com.ecoapp.reward.exception.DepositRefusedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

/**
 * Asks scan-service whether a barcode is returnable for deposit. Fails closed: every problem
 * (bad format, missing token, network error, timeout, non-200, malformed body) becomes
 * DepositRefusedException.unavailable(). Barcodes and exception messages are never logged.
 */
@Component
public class ScanDepositClient {

    private static final Logger log = LoggerFactory.getLogger(ScanDepositClient.class);
    private static final Pattern BARCODE = Pattern.compile("^[0-9]{8,14}$");
    static final String TOKEN_HEADER = "X-Internal-Token";

    public record DepositCheck(boolean eligible, double depositValue) {}

    /** Wire format; boxed types so a missing field is detected instead of defaulting to false/0. */
    record Body(Boolean eligible, Double depositValue) {}

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String token;

    public ScanDepositClient(@Qualifier("scanRestTemplate") RestTemplate restTemplate,
                             @Value("${scan.service.url}") String baseUrl,
                             @Value("${internal.service.token:}") String token) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token == null ? "" : token;
    }

    public DepositCheck check(String barcode) {
        if (barcode == null || !BARCODE.matcher(barcode).matches() || token.isEmpty()) {
            throw DepositRefusedException.unavailable();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(TOKEN_HEADER, token);
            ResponseEntity<Body> response = restTemplate.exchange(
                    baseUrl + "/internal/scan/deposit/" + barcode,
                    HttpMethod.GET, new HttpEntity<>(headers), Body.class);
            Body body = response.getBody();
            if (response.getStatusCode().value() != 200 || body == null
                    || body.eligible() == null || body.depositValue() == null
                    || !Double.isFinite(body.depositValue()) || body.depositValue() < 0
                    || (body.eligible() && body.depositValue() <= 0)) {
                log.warn("Deposit verification returned an unusable response");
                throw DepositRefusedException.unavailable();
            }
            return new DepositCheck(body.eligible(), body.depositValue());
        } catch (DepositRefusedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Deposit verification failed: {}", e.getClass().getSimpleName());
            throw DepositRefusedException.unavailable();
        }
    }
}
