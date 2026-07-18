package com.teleconnect.usage.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class AlertResponse {
    private List<Alert> alerts;

    @Data
    @AllArgsConstructor
    public static class Alert {
        private String usageType;
        private double percentageUsed;
        private String thresholdLevel; // WARNING (>=80%) / CRITICAL (>=90%)
    }
}