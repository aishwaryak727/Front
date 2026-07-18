
package com.teleconnect.plan.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import com.teleconnect.plan.dto.response.MessageResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MessageResponse> handleDataIntegrity(
            DataIntegrityViolationException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("name"))
            return ResponseEntity.status(400)
                .body(new MessageResponse("name is required"));
        if (msg.contains("planPrice"))
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "planPrice must be a positive number"));
        if (msg.contains("validityDays"))
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "validityDays must be a positive integer"));
        if (msg.contains("quota"))
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "quota must be a positive number"));
        if (msg.contains("price"))
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "price must be a positive number"));
        return ResponseEntity.status(400)
            .body(new MessageResponse(
                "Required field is missing or invalid"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<MessageResponse> handleBadJson(
            HttpMessageNotReadableException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("PlanType") || msg.contains("PlanStatus"))
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "type must be Postpaid or Prepaid"));
        if (msg.contains("AddOnType"))
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "type must be DataTopup, ISDPack, RoamingPack, or SMSPack"));
        if (msg.contains("RenewalType"))
            return ResponseEntity.status(400)
                .body(new MessageResponse(
                    "renewalType must be AutoRenew or Manual"));
        return ResponseEntity.status(400)
            .body(new MessageResponse("Invalid input value"));
    }
}