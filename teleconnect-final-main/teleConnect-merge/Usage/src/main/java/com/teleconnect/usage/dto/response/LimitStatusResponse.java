package com.teleconnect.usage.dto.response;

import lombok.Data;

@Data
public class LimitStatusResponse {
    private Long lineId;
    private DataStatus data;
    private VoiceStatus voice;
    private SmsStatus sms;

    @Data
    public static class DataStatus {
        private double usedMb;
        private double limitMb;
        private String status; // WITHIN_LIMIT / LIMIT_EXCEEDED
    }

    @Data
    public static class VoiceStatus {
        private double usedMin;
        private double limitMin;
        private String status;
    }

    @Data
    public static class SmsStatus {
        private int used;
        private int limit;
        private String status;
    }
}