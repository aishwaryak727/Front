package com.teleconnect.analytics_service.service;

import com.teleconnect.analytics_service.dto.response.*;

import java.io.IOException;

public interface ExportService {

    byte[] exportReportAsPdf(Long reportId) throws IOException;
    byte[] exportReportAsCsv(Long reportId);

    byte[] exportDashboardAsPdf(DashboardResponse data) throws IOException;

    byte[] exportArpuAsPdf(ARPUReportResponse data) throws IOException;
    byte[] exportArpuAsCsv(ARPUReportResponse data);

    byte[] exportChurnAsPdf(ChurnReportResponse data) throws IOException;
    byte[] exportChurnAsCsv(ChurnReportResponse data);

    byte[] exportNetworkUtilisationAsPdf(NetworkUtilisationResponse data) throws IOException;
    byte[] exportNetworkUtilisationAsCsv(NetworkUtilisationResponse data);

    byte[] exportSLAComplianceAsPdf(SLAComplianceResponse data) throws IOException;
    byte[] exportSLAComplianceAsCsv(SLAComplianceResponse data);

    byte[] exportCollectionEfficiencyAsPdf(CollectionEfficiencyResponse data) throws IOException;
    byte[] exportCollectionEfficiencyAsCsv(CollectionEfficiencyResponse data);

    byte[] exportSubscriberGrowthAsPdf(SubscriberGrowthResponse data) throws IOException;
    byte[] exportSubscriberGrowthAsCsv(SubscriberGrowthResponse data);
}
