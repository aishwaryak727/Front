package com.teleconnect.analytics_service.dto.response;

public class NetworkUtilisationResponse {

    private Long cycleId;
    private String region;
    private long totalDataUsedMb;
    private long totalVoiceUsedMin;
    private long totalSmsUsed;
    private long subscriberCount;
    private double avgDataPerSubscriberMb;
    private double avgVoicePerSubscriberMin;

    public NetworkUtilisationResponse() {}

    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public long getTotalDataUsedMb() { return totalDataUsedMb; }
    public void setTotalDataUsedMb(long totalDataUsedMb) { this.totalDataUsedMb = totalDataUsedMb; }

    public long getTotalVoiceUsedMin() { return totalVoiceUsedMin; }
    public void setTotalVoiceUsedMin(long totalVoiceUsedMin) { this.totalVoiceUsedMin = totalVoiceUsedMin; }

    public long getTotalSmsUsed() { return totalSmsUsed; }
    public void setTotalSmsUsed(long totalSmsUsed) { this.totalSmsUsed = totalSmsUsed; }

    public long getSubscriberCount() { return subscriberCount; }
    public void setSubscriberCount(long subscriberCount) { this.subscriberCount = subscriberCount; }

    public double getAvgDataPerSubscriberMb() { return avgDataPerSubscriberMb; }
    public void setAvgDataPerSubscriberMb(double avgDataPerSubscriberMb) { this.avgDataPerSubscriberMb = avgDataPerSubscriberMb; }

    public double getAvgVoicePerSubscriberMin() { return avgVoicePerSubscriberMin; }
    public void setAvgVoicePerSubscriberMin(double avgVoicePerSubscriberMin) { this.avgVoicePerSubscriberMin = avgVoicePerSubscriberMin; }
}
