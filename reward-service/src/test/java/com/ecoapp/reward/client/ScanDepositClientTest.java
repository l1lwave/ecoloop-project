package com.ecoapp.reward.client;

import com.ecoapp.reward.exception.DepositRefusedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ScanDepositClientTest {

    private static final String BARCODE = "8718951234567";
    private static final String URL = "http://scan:8082/internal/scan/deposit/" + BARCODE;
    private static final String TOKEN = "test-token-not-a-secret";

    private final RestTemplate rest = new RestTemplate();
    private final MockRestServiceServer server = MockRestServiceServer.createServer(rest);

    private ScanDepositClient client(String token) {
        return new ScanDepositClient(rest, "http://scan:8082/", token);
    }

    private static void assertUnavailable(Runnable r) {
        DepositRefusedException e = assertThrows(DepositRefusedException.class, r::run);
        assertEquals(503, e.getStatus().value());
        assertEquals("DEPOSIT_UNAVAILABLE", e.getCode());
    }

    @Test
    void check_sendsTokenHeaderAndParsesEligibleBody() {
        server.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Token", TOKEN))
                .andRespond(withSuccess("{\"eligible\":true,\"depositValue\":0.15}", MediaType.APPLICATION_JSON));

        var r = client(TOKEN).check(BARCODE);

        assertTrue(r.eligible());
        assertEquals(0.15, r.depositValue(), 0.0001);
        server.verify();
    }

    @Test
    void check_parsesNotEligibleBody() {
        server.expect(requestTo(URL))
                .andRespond(withSuccess("{\"eligible\":false,\"depositValue\":0.0}", MediaType.APPLICATION_JSON));
        assertFalse(client(TOKEN).check(BARCODE).eligible());
    }

    @Test
    void check_scan401IsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertUnavailable(() -> client(TOKEN).check(BARCODE));
    }

    @Test
    void check_scan500IsUnavailable() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        assertUnavailable(() -> client(TOKEN).check(BARCODE));
    }

    @Test
    void check_timeoutIsUnavailable() {
        server.expect(requestTo(URL)).andRespond(request -> { throw new SocketTimeoutException("read timed out"); });
        assertUnavailable(() -> client(TOKEN).check(BARCODE));
    }

    @Test
    void check_malformedOrInconsistentBodyIsUnavailable() {
        for (String body : new String[]{"{}", "{\"eligible\":true}", "{\"eligible\":true,\"depositValue\":0}",
                "{\"eligible\":false,\"depositValue\":-1}", "not json"}) {
            server.reset();
            server.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertUnavailable(() -> client(TOKEN).check(BARCODE));
        }
    }

    @Test
    void check_badBarcodeIsRejectedBeforeAnyCall() {
        for (String bad : new String[]{null, "", "123", "../etc/passwd", "12345678901234567", "8718951234567?x=1"}) {
            assertUnavailable(() -> client(TOKEN).check(bad));
        }
        server.verify(); // no request was expected or made
    }

    @Test
    void check_emptyTokenFailsClosedWithoutCall() {
        assertUnavailable(() -> client("").check(BARCODE));
        server.verify();
    }
}
