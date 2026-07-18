package com.teleconnect.analytics_service.enums;

public enum FaultPriority {
    LOW, MEDIUM, HIGH, CRITICAL;

    public long getSlaHours() {
        return switch (this) {
            case CRITICAL -> 4L;
            case HIGH -> 8L;
            case MEDIUM -> 24L;
            case LOW -> 72L;
        };
    }
}
