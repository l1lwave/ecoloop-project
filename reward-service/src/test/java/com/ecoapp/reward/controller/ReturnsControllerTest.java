package com.ecoapp.reward.controller;

import com.ecoapp.reward.dto.ReturnsSummaryQuery;
import com.ecoapp.reward.dto.ReturnsSummaryResponse;
import com.ecoapp.reward.service.ReturnsService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReturnsControllerTest {

    private final ReturnsService service = mock(ReturnsService.class);
    private final ReturnsController controller = new ReturnsController(service);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void summary_usesHeaderIdentityAndParsedYear() {
        ReturnsSummaryResponse body = new ReturnsSummaryResponse(2026, List.of(), 0, 0.0, 0.0);
        when(service.getSummary("u1", 2026)).thenReturn(body);

        ResponseEntity<ReturnsSummaryResponse> r = controller.summary("u1", new ReturnsSummaryQuery("2026"));

        assertEquals(200, r.getStatusCode().value());
        assertEquals(body, r.getBody());
        verify(service).getSummary("u1", 2026);
    }

    @Test
    void query_rejectsYearsOutsideTwoThousandToTwoThousandOneHundred() {
        assertFalse(validator.validate(new ReturnsSummaryQuery("1999")).isEmpty());
        assertFalse(validator.validate(new ReturnsSummaryQuery("2101")).isEmpty());
        assertFalse(validator.validate(new ReturnsSummaryQuery("abcd")).isEmpty());
        assertFalse(validator.validate(new ReturnsSummaryQuery(null)).isEmpty());
        assertTrue(validator.validate(new ReturnsSummaryQuery("2000")).isEmpty());
        assertTrue(validator.validate(new ReturnsSummaryQuery("2100")).isEmpty());
    }
}
