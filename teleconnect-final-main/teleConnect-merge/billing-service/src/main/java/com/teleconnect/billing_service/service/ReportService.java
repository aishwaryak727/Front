package com.teleconnect.billing_service.service;

import com.teleconnect.billing_service.dto.response.CollectionReportResponse;
import com.teleconnect.billing_service.dto.response.DisputeSummaryResponse;
import com.teleconnect.billing_service.dto.response.OverdueReportResponse;

import java.time.LocalDate;

public interface ReportService {

    OverdueReportResponse getOverdueReport(String region, String agingBucket);

    CollectionReportResponse getCollectionReport(LocalDate fromDate, LocalDate toDate, String region);

    DisputeSummaryResponse getDisputeSummary(LocalDate fromDate, LocalDate toDate);
}
