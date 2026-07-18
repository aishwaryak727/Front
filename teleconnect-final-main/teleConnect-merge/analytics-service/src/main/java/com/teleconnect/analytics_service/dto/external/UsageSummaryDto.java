package com.teleconnect.analytics_service.dto.external;

/**
 * Lightweight projection of UsageSummary, as returned by the Usage &
 * Consumption Tracking module's analytics-facing API.
 */
public class UsageSummaryDto {

    private Long summaryId;
    private Long lineId;
    private Long billingCycleId;
    private Long dataUsedMb;
    private Long voiceUsedMin;
    private Long smsUsed;

    public UsageSummaryDto() {}

    public Long getSummaryId() { return summaryId; }
    public void setSummaryId(Long summaryId) { this.summaryId = summaryId; }

    public Long getLineId() { return lineId; }
    public void setLineId(Long lineId) { this.lineId = lineId; }

    public Long getBillingCycleId() { return billingCycleId; }
    public void setBillingCycleId(Long billingCycleId) { this.billingCycleId = billingCycleId; }

    public Long getDataUsedMb() { return dataUsedMb; }
    public void setDataUsedMb(Long dataUsedMb) { this.dataUsedMb = dataUsedMb; }

    public Long getVoiceUsedMin() { return voiceUsedMin; }
    public void setVoiceUsedMin(Long voiceUsedMin) { this.voiceUsedMin = voiceUsedMin; }

    public Long getSmsUsed() { return smsUsed; }
    public void setSmsUsed(Long smsUsed) { this.smsUsed = smsUsed; }
}
