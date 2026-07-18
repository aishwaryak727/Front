package com.teleconnect.fault.exception;

import com.teleconnect.fault.dto.response.MessageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Handles @Valid annotation errors — 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleValidation(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(400).body(new MessageResponse(message));
    }

    // Handles RuntimeException — 404 (when "not found") and 400 otherwise
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleRuntime(RuntimeException ex) {
        String msg = ex.getMessage();
        int code = (msg != null && msg.toLowerCase().contains("not found")) ? 404 : 400;
        return ResponseEntity.status(code).body(new MessageResponse(msg));
    }
}
