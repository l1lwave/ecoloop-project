package com.ecoapp.scan.controller;

import com.ecoapp.scan.dto.ScanHistoryResponse;
import com.ecoapp.scan.dto.ScanRequest;
import com.ecoapp.scan.dto.ScanResponse;
import com.ecoapp.scan.service.ScanService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScanControllerTest {

    private static final String BARCODE = "8718000000001";

    private final ScanService service = mock(ScanService.class);
    private final ScanController controller = new ScanController(service);

    @Test
    void identify_passesHeaderUserIdToService() {
        ScanResponse body = new ScanResponse(BARCODE, "Spa", "Spa", "01 PET", "PET", true, 0.15, "Lever in.");
        when(service.identify(BARCODE, "nl", "user-1")).thenReturn(body);

        ResponseEntity<ScanResponse> r = controller.identify("user-1", new ScanRequest(BARCODE, "nl"));

        assertEquals(200, r.getStatusCode().value());
        assertEquals(body, r.getBody());
        verify(service).identify(BARCODE, "nl", "user-1");
    }

    @Test
    void history_passesHeaderUserIdAndPaging() {
        ScanHistoryResponse body = new ScanHistoryResponse(List.of(), 1, 20, false);
        when(service.history("user-1", 1, 20)).thenReturn(body);

        ResponseEntity<ScanHistoryResponse> r = controller.history("user-1", 1, 20);

        assertEquals(200, r.getStatusCode().value());
        assertEquals(body, r.getBody());
    }
}
