package com.teleconnect.usage.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class AnalyticsTrendResponse {
    private Long lineId;
    private List<CycleTrend> trend;

    @Data
    public static class CycleTrend {
        private Long billingCycleId;
        private double dataUsedMb;
        private double voiceUsedMin;
        private int smsUsed;
    }
}
