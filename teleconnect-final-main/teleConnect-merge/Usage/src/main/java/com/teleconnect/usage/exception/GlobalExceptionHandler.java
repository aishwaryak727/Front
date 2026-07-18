package com.teleconnect.usage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
        // 404 — thrown when line/cycle/summary not found
        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<Map<String, String>> handleNotFound(
                        ResourceNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("message", ex.getMessage()));
        }

        // 400 — thrown when usageType enum conversion fails
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, String>> handleBadRequest(
                        IllegalArgumentException ex) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", ex.getMessage()));
        }

        // 400 — thrown for @Valid annotation failures
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidation(
                        MethodArgumentNotValidException ex) {
                String msg = ex.getBindingResult().getFieldErrors().stream()
                                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                                .findFirst().orElse("Validation error");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(Map.of("message", msg));
        }

        // 500 — catch-all for unexpected errors
        @ExceptionHandler(Exception.class)
        public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("message", "Internal server error: " + ex.getMessage()));
        }
}