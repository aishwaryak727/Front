package com.teleconnect.analytics_service.service;

import com.teleconnect.analytics_service.dto.request.ReportGenerationRequest;
import com.teleconnect.analytics_service.dto.response.TelecomReportResponse;
import com.teleconnect.analytics_service.enums.ReportScope;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface ReportService {
    TelecomReportResponse generateReport(ReportGenerationRequest request);
    TelecomReportResponse getReportById(Long reportId);
    Page<TelecomReportResponse> listReports(ReportScope scope, LocalDate from, LocalDate to, int page, int size);
}
