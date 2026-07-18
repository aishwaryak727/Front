package com.teleconnect.analytics_service.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardResponse {

    private KPIMetrics kpis;
    private ChartData charts;
    private RegionalAnalysis regions;
    private SegmentAnalysis segments;

    public DashboardResponse() {}

    public static class KPIMetrics {
        private long activeSubscribers;
        private double churnRate;
        private BigDecimal arpu;
        private double collectionEfficiency;
        private double slaCompliance;
        private long dataConsumption;
        private long faultCount;
        private double disputeRate;

        public KPIMetrics() {}

        // Getters/Setters
        public long getActiveSubscribers() { return activeSubscribers; }
        public void setActiveSubscribers(long activeSubscribers) { this.activeSubscribers = activeSubscribers; }

        public double getChurnRate() { return churnRate; }
        public void setChurnRate(double churnRate) { this.churnRate = churnRate; }

        public BigDecimal getArpu() { return arpu; }
        public void setArpu(BigDecimal arpu) { this.arpu = arpu; }

        public double getCollectionEfficiency() { return collectionEfficiency; }
        public void setCollectionEfficiency(double collectionEfficiency) { this.collectionEfficiency = collectionEfficiency; }

        public double getSlaCompliance() { return slaCompliance; }
        public void setSlaCompliance(double slaCompliance) { this.slaCompliance = slaCompliance; }

        public long getDataConsumption() { return dataConsumption; }
        public void setDataConsumption(long dataConsumption) { this.dataConsumption = dataConsumption; }

        public long getFaultCount() { return faultCount; }
        public void setFaultCount(long faultCount) { this.faultCount = faultCount; }

        public double getDisputeRate() { return disputeRate; }
        public void setDisputeRate(double disputeRate) { this.disputeRate = disputeRate; }
    }

    public static class ChartData {
        private LineChartData consumptionTrend;
        private BarChartData arpuByAccountType;
        private PieChartData churnBySegment;
        private BarChartData collectionOverdueAgeing;
        private LineChartData subscriberGrowthTrend;
        private BarChartData faultFrequency;

        public ChartData() {}

        // Getters/Setters
        public LineChartData getConsumptionTrend() { return consumptionTrend; }
        public void setConsumptionTrend(LineChartData consumptionTrend) { this.consumptionTrend = consumptionTrend; }

        public BarChartData getArpuByAccountType() { return arpuByAccountType; }
        public void setArpuByAccountType(BarChartData arpuByAccountType) { this.arpuByAccountType = arpuByAccountType; }

        public PieChartData getChurnBySegment() { return churnBySegment; }
        public void setChurnBySegment(PieChartData churnBySegment) { this.churnBySegment = churnBySegment; }

        public BarChartData getCollectionOverdueAgeing() { return collectionOverdueAgeing; }
        public void setCollectionOverdueAgeing(BarChartData collectionOverdueAgeing) { this.collectionOverdueAgeing = collectionOverdueAgeing; }

        public LineChartData getSubscriberGrowthTrend() { return subscriberGrowthTrend; }
        public void setSubscriberGrowthTrend(LineChartData subscriberGrowthTrend) { this.subscriberGrowthTrend = subscriberGrowthTrend; }

        public BarChartData getFaultFrequency() { return faultFrequency; }
        public void setFaultFrequency(BarChartData faultFrequency) { this.faultFrequency = faultFrequency; }
    }

    public static class LineChartData {
        private String title;
        private List<String> labels;
        private List<Long> data;

        public LineChartData() {}
        public LineChartData(String title, List<String> labels, List<Long> data) {
            this.title = title;
            this.labels = labels;
            this.data = data;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getLabels() { return labels; }
        public void setLabels(List<String> labels) { this.labels = labels; }
        public List<Long> getData() { return data; }
        public void setData(List<Long> data) { this.data = data; }
    }

    public static class BarChartData {
        private String title;
        private List<String> labels;
        private List<Double> values;

        public BarChartData() {}
        public BarChartData(String title, List<String> labels, List<Double> values) {
            this.title = title;
            this.labels = labels;
            this.values = values;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getLabels() { return labels; }
        public void setLabels(List<String> labels) { this.labels = labels; }
        public List<Double> getValues() { return values; }
        public void setValues(List<Double> values) { this.values = values; }
    }

    public static class PieChartData {
        private String title;
        private List<String> labels;
        private List<Double> values;

        public PieChartData() {}
        public PieChartData(String title, List<String> labels, List<Double> values) {
            this.title = title;
            this.labels = labels;
            this.values = values;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public List<String> getLabels() { return labels; }
        public void setLabels(List<String> labels) { this.labels = labels; }
        public List<Double> getValues() { return values; }
        public void setValues(List<Double> values) { this.values = values; }
    }

    public static class RegionalAnalysis {
        private Map<String, Long> subscribersByRegion;
        private Map<String, Double> churnByRegion;
        private Map<String, BigDecimal> arpuByRegion;

        public RegionalAnalysis() {}

        public Map<String, Long> getSubscribersByRegion() { return subscribersByRegion; }
        public void setSubscribersByRegion(Map<String, Long> subscribersByRegion) { this.subscribersByRegion = subscribersByRegion; }

        public Map<String, Double> getChurnByRegion() { return churnByRegion; }
        public void setChurnByRegion(Map<String, Double> churnByRegion) { this.churnByRegion = churnByRegion; }

        public Map<String, BigDecimal> getArpuByRegion() { return arpuByRegion; }
        public void setArpuByRegion(Map<String, BigDecimal> arpuByRegion) { this.arpuByRegion = arpuByRegion; }
    }

    public static class SegmentAnalysis {
        private Map<String, Long> subscribersByAccountType;
        private Map<String, BigDecimal> arpuByAccountType;
        private Map<String, Double> churnByAccountType;

        public SegmentAnalysis() {}

        public Map<String, Long> getSubscribersByAccountType() { return subscribersByAccountType; }
        public void setSubscribersByAccountType(Map<String, Long> subscribersByAccountType) { this.subscribersByAccountType = subscribersByAccountType; }

        public Map<String, BigDecimal> getArpuByAccountType() { return arpuByAccountType; }
        public void setArpuByAccountType(Map<String, BigDecimal> arpuByAccountType) { this.arpuByAccountType = arpuByAccountType; }

        public Map<String, Double> getChurnByAccountType() { return churnByAccountType; }
        public void setChurnByAccountType(Map<String, Double> churnByAccountType) { this.churnByAccountType = churnByAccountType; }
    }

    // Main getters/setters
    public KPIMetrics getKpis() { return kpis; }
    public void setKpis(KPIMetrics kpis) { this.kpis = kpis; }

    public ChartData getCharts() { return charts; }
    public void setCharts(ChartData charts) { this.charts = charts; }

    public RegionalAnalysis getRegions() { return regions; }
    public void setRegions(RegionalAnalysis regions) { this.regions = regions; }

    public SegmentAnalysis getSegments() { return segments; }
    public void setSegments(SegmentAnalysis segments) { this.segments = segments; }
}
