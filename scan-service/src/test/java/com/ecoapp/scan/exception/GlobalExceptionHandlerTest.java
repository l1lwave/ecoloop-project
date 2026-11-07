package com.ecoapp.scan.exception;

import com.ecoapp.scan.exception.GlobalExceptionHandler;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    @Test
    void handleInvalidRequest_returnsGenericBadRequest() {
        ResponseEntity<Map<String, String>> r = new GlobalExceptionHandler()
                .handleInvalidRequest(new ConstraintViolationException("page: must be greater than or equal to 0", Set.of()));

        assertEquals(400, r.getStatusCode().value());
        assertEquals(Map.of("error", "INVALID_REQUEST", "message", "Request validation failed"), r.getBody());
    }
}
