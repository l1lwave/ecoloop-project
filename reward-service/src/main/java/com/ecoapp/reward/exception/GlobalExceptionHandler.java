package com.ecoapp.reward.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.BindException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_REQUEST", "message", "Request validation failed"));
    }

    /** Binding failures of query parameters (for example a @ModelAttribute with @Valid); same body as body validation. */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> handleBind(BindException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_REQUEST", "message", "Request validation failed"));
    }

    @ExceptionHandler(DepositRefusedException.class)
    public ResponseEntity<Map<String, String>> handleDepositRefused(DepositRefusedException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("error", ex.getCode(), "message", ex.getClientMessage()));
    }

    /** Same body for every referral rule violation so codes cannot be probed. */
    @ExceptionHandler(InvalidReferralException.class)
    public ResponseEntity<Map<String, String>> handleInvalidReferral(InvalidReferralException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_REFERRAL", "message", "Referral could not be claimed"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, String>> handleMissingHeader(MissingRequestHeaderException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "UNAUTHORIZED", "message", "Missing verified identity"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_REQUEST", "message", "Malformed request body"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("error", "METHOD_NOT_ALLOWED", "message", "Method not allowed"));
    }

    /** Keeps the status of an intentional ResponseStatusException; the reason text is not echoed. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return clientOrServerError(ex.getStatusCode());
    }

    /** Logs the cause server-side only; the client never sees exception messages or stack traces. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        // Other Spring MVC exceptions (415, 404, type mismatch, ...) carry their own status; keep it.
        if (ex instanceof ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            return clientOrServerError(er.getStatusCode());
        }
        log.error("Unhandled exception in reward-service", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "INTERNAL_ERROR", "message", "Unexpected error"));
    }

    private ResponseEntity<Map<String, String>> clientOrServerError(HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return ResponseEntity.status(status)
                    .body(Map.of("error", "INTERNAL_ERROR", "message", "Unexpected error"));
        }
        return ResponseEntity.status(status)
                .body(Map.of("error", "REQUEST_REJECTED", "message", "Request could not be processed"));
    }
}
