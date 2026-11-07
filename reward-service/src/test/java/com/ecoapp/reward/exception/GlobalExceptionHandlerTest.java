package com.ecoapp.reward.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGeneral_hidesInternalDetails() {
        ResponseEntity<Map<String, String>> r =
                handler.handleGeneral(new RuntimeException("jdbc:mysql://secret-host"));

        assertEquals(500, r.getStatusCode().value());
        assertFalse(r.getBody().toString().contains("secret-host"));
    }

    @Test
    void handleUnreadable_returns400() {
        var r = handler.handleUnreadable(new HttpMessageNotReadableException("bad json at line 1", (HttpInputMessage) null));
        assertEquals(400, r.getStatusCode().value());
        assertEquals("Malformed request body", r.getBody().get("message"));
    }

    @Test
    void handleMethodNotSupported_returns405() {
        var r = handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("GET"));
        assertEquals(405, r.getStatusCode().value());
        assertEquals("METHOD_NOT_ALLOWED", r.getBody().get("error"));
    }

    @Test
    void handleResponseStatus_keepsStatusAndHidesReason() {
        var r = handler.handleResponseStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "internal detail"));
        assertEquals(404, r.getStatusCode().value());
        assertFalse(r.getBody().toString().contains("internal detail"));
    }

    @Test
    void handleGeneral_keepsStatusOfSpringClientErrors() {
        var r = handler.handleGeneral(new HttpMediaTypeNotSupportedException("nope"));
        assertEquals(415, r.getStatusCode().value());
    }
}
