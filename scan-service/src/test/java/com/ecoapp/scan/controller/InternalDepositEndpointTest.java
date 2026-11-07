package com.ecoapp.scan.controller;

import com.ecoapp.scan.dto.DepositCheckResponse;
import com.ecoapp.scan.exception.GlobalExceptionHandler;
import com.ecoapp.scan.security.InternalTokenFilter;
import com.ecoapp.scan.security.RequireUserIdFilter;
import com.ecoapp.scan.service.ScanService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalDepositEndpointTest {

    private static final String TOKEN = "test-token-not-a-secret";
    private static final String BARCODE = "8718000000001";

    private final ScanService service = mock(ScanService.class);

    private MockMvc mvc(String configuredToken) {
        return MockMvcBuilders.standaloneSetup(new InternalDepositController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new InternalTokenFilter(configuredToken), new RequireUserIdFilter())
                .build();
    }

    @Test
    void missingToken_returns401WithGenericBody() throws Exception {
        mvc(TOKEN).perform(get("/internal/scan/deposit/" + BARCODE))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        verifyNoInteractions(service);
    }

    @Test
    void wrongToken_returns401() throws Exception {
        mvc(TOKEN).perform(get("/internal/scan/deposit/" + BARCODE).header("X-Internal-Token", "wrong"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void emptyConfiguredToken_refusesEvenAnEmptyOrMatchingHeader() throws Exception {
        mvc("").perform(get("/internal/scan/deposit/" + BARCODE).header("X-Internal-Token", ""))
                .andExpect(status().isUnauthorized());
        mvc("").perform(get("/internal/scan/deposit/" + BARCODE))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void badBarcodeFormat_returns400BeforeLookup() throws Exception {
        for (String bad : new String[]{"123", "12345678901234567", "abcdefgh12"}) {
            mvc(TOKEN).perform(get("/internal/scan/deposit/" + bad).header("X-Internal-Token", TOKEN))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void unknownBarcode_returns200NotEligible() throws Exception {
        when(service.checkDeposit(BARCODE)).thenReturn(new DepositCheckResponse(false, 0.0));
        mvc(TOKEN).perform(get("/internal/scan/deposit/" + BARCODE).header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.depositValue").value(0.0));
    }

    @Test
    void eligibleProduct_returns200WithValue() throws Exception {
        when(service.checkDeposit(BARCODE)).thenReturn(new DepositCheckResponse(true, 0.15));
        mvc(TOKEN).perform(get("/internal/scan/deposit/" + BARCODE).header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.depositValue").value(0.15));
    }

    @Test
    void nonEligibleProduct_returns200NotEligible() throws Exception {
        when(service.checkDeposit("5000000000017")).thenReturn(new DepositCheckResponse(false, 0.0));
        mvc(TOKEN).perform(get("/internal/scan/deposit/5000000000017").header("X-Internal-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false));
    }
}
