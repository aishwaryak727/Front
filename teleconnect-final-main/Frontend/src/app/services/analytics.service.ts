import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface KPIMetrics {
  activeSubscribers: number;
  churnRate: number;
  arpu: number;
  collectionEfficiency: number;
  slaCompliance: number;
  dataConsumption: number;
  faultCount: number;
  disputeRate: number;
}

export interface ChartDetail {
  title: string;
  labels: string[];
  data?: number[];
  values?: number[];
}

export interface ChartData {
  consumptionTrend: ChartDetail;
  arpuByAccountType: ChartDetail;
  churnBySegment: ChartDetail;
  collectionOverdueAgeing: ChartDetail;
  subscriberGrowthTrend: ChartDetail;
  faultFrequency: ChartDetail;
}

export interface RegionalAnalysis {
  subscribersByRegion: { [key: string]: number };
  churnByRegion: { [key: string]: number };
  arpuByRegion: { [key: string]: number };
}

export interface SegmentAnalysis {
  subscribersByAccountType: { [key: string]: number };
  arpuByAccountType: { [key: string]: number };
  churnByAccountType: { [key: string]: number };
}

export interface DashboardResponse {
  kpis: KPIMetrics;
  charts: ChartData;
  regions: RegionalAnalysis;
  segments: SegmentAnalysis;
}

export interface ARPUReportResponse {
  cycleId: number;
  scope: string;
  scopeValue: string;
  totalRevenue: number;
  totalSubscribers: number;
  arpuValue: number;
  periodStart: string;
  periodEnd: string;
}

export interface ChurnReportResponse {
  periodStart: string;
  periodEnd: string;
  region: string;
  subscribersAtStart: number;
  subscribersAtEnd: number;
  subscribersLost: number;
  churnRate: number;
}

export interface NetworkUtilisationResponse {
  cycleId: number;
  region: string;
  totalDataCapacityMb: number;
  totalDataConsumedMb: number;
  overallUtilisationPercentage: number;
  periodStart: string;
  periodEnd: string;
}

export interface SLAComplianceResponse {
  periodStart: string;
  periodEnd: string;
  totalTicketsRaised: number;
  ticketsResolvedInSLA: number;
  slaCompliancePercentage: number;
}

export interface CollectionEfficiencyResponse {
  cycleId: number;
  totalInvoicedAmount: number;
  totalCollectedAmount: number;
  collectionEfficiencyPercentage: number;
  periodStart: string;
  periodEnd: string;
}

export interface SubscriberGrowthResponse {
  periodStart: string;
  periodEnd: string;
  startingSubscribers: number;
  endingSubscribers: number;
  newSubscribersAcquired: number;
  netGrowthRatePercentage: number;
}

export interface TelecomReportResponse {
  reportId: number;
  scope: string; // REGION, PLAN, SEGMENT, PERIOD
  scopeValue: string;
  periodStart: string;
  periodEnd: string;
  metrics: string; // JSON string of metrics
  generatedDate: string;
  generatedBy: number;
}

export interface ReportGenerationRequest {
  scope: string;
  scopeValue: string;
  periodStart: string;
  periodEnd: string;
  generatedBy: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class AnalyticsService {
  private readonly baseUrl = 'http://localhost:9090/teleConnect/api';

  constructor(private http: HttpClient) {}

  // Dashboard Aggregates
  getDashboard(startDate?: string, endDate?: string, cycleId = 1): Observable<DashboardResponse> {
    let params = new HttpParams().set('cycleId', cycleId.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<DashboardResponse>>(`${this.baseUrl}/dashboard`, { params }).pipe(
      map(res => res.data)
    );
  }

  getKPIMetrics(startDate?: string, endDate?: string, cycleId = 1): Observable<KPIMetrics> {
    let params = new HttpParams().set('cycleId', cycleId.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<KPIMetrics>>(`${this.baseUrl}/dashboard/kpis`, { params }).pipe(
      map(res => res.data)
    );
  }

  getChartData(startDate?: string, endDate?: string, cycleId = 1): Observable<ChartData> {
    let params = new HttpParams().set('cycleId', cycleId.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<ChartData>>(`${this.baseUrl}/dashboard/charts`, { params }).pipe(
      map(res => res.data)
    );
  }

  // Live Ad-Hoc Computations
  getARPU(cycleId: number, scope = 'PERIOD', scopeValue = 'ALL'): Observable<ARPUReportResponse> {
    let params = new HttpParams()
      .set('cycleId', cycleId.toString())
      .set('scope', scope)
      .set('scopeValue', scopeValue);
    return this.http.get<ApiResponse<ARPUReportResponse>>(`${this.baseUrl}/reports/arpu`, { params }).pipe(
      map(res => res.data)
    );
  }

  getChurn(periodStart: string, periodEnd: string, region?: string): Observable<ChurnReportResponse> {
    let params = new HttpParams()
      .set('periodStart', periodStart)
      .set('periodEnd', periodEnd);
    if (region) params = params.set('region', region);
    return this.http.get<ApiResponse<ChurnReportResponse>>(`${this.baseUrl}/reports/churn`, { params }).pipe(
      map(res => res.data)
    );
  }

  getNetworkUtilisation(cycleId: number, region?: string): Observable<NetworkUtilisationResponse> {
    let params = new HttpParams().set('cycleId', cycleId.toString());
    if (region) params = params.set('region', region);
    return this.http.get<ApiResponse<NetworkUtilisationResponse>>(`${this.baseUrl}/reports/network-utilisation`, { params }).pipe(
      map(res => res.data)
    );
  }

  getSLACompliance(periodStart: string, periodEnd: string): Observable<SLAComplianceResponse> {
    let params = new HttpParams()
      .set('periodStart', periodStart)
      .set('periodEnd', periodEnd);
    return this.http.get<ApiResponse<SLAComplianceResponse>>(`${this.baseUrl}/reports/sla-compliance`, { params }).pipe(
      map(res => res.data)
    );
  }

  getCollectionEfficiency(cycleId: number): Observable<CollectionEfficiencyResponse> {
    let params = new HttpParams().set('cycleId', cycleId.toString());
    return this.http.get<ApiResponse<CollectionEfficiencyResponse>>(`${this.baseUrl}/reports/collection-efficiency`, { params }).pipe(
      map(res => res.data)
    );
  }

  getSubscriberGrowth(periodStart: string, periodEnd: string): Observable<SubscriberGrowthResponse> {
    let params = new HttpParams()
      .set('periodStart', periodStart)
      .set('periodEnd', periodEnd);
    return this.http.get<ApiResponse<SubscriberGrowthResponse>>(`${this.baseUrl}/reports/subscriber-growth`, { params }).pipe(
      map(res => res.data)
    );
  }

  // Saved Historical Reports Snapshots
  generateReport(req: ReportGenerationRequest): Observable<TelecomReportResponse> {
    return this.http.post<ApiResponse<TelecomReportResponse>>(`${this.baseUrl}/reports/generate`, req).pipe(
      map(res => res.data)
    );
  }

  getReport(reportId: number): Observable<TelecomReportResponse> {
    return this.http.get<ApiResponse<TelecomReportResponse>>(`${this.baseUrl}/reports/${reportId}`).pipe(
      map(res => res.data)
    );
  }

  listReports(scope?: string, from?: string, to?: string, page = 0, size = 20): Observable<Page<TelecomReportResponse>> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (scope) params = params.set('scope', scope);
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<ApiResponse<Page<TelecomReportResponse>>>(`${this.baseUrl}/reports`, { params }).pipe(
      map(res => res.data)
    );
  }

  // PDF / CSV Exports (Binary Blobs)
  exportDashboard(cycleId: number, startDate?: string, endDate?: string): Observable<Blob> {
    let params = new HttpParams().set('cycleId', cycleId.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get(`${this.baseUrl}/reports/dashboard/export`, { params, responseType: 'blob' });
  }

  exportSavedReport(reportId: number, format = 'pdf'): Observable<Blob> {
    let params = new HttpParams().set('format', format);
    return this.http.get(`${this.baseUrl}/reports/${reportId}/export`, { params, responseType: 'blob' });
  }

  exportArpu(cycleId: number, scope = 'PERIOD', scopeValue = 'ALL', format = 'pdf'): Observable<Blob> {
    let params = new HttpParams()
      .set('cycleId', cycleId.toString())
      .set('scope', scope)
      .set('scopeValue', scopeValue)
      .set('format', format);
    return this.http.get(`${this.baseUrl}/reports/arpu/export`, { params, responseType: 'blob' });
  }

  exportChurn(periodStart: string, periodEnd: string, region?: string, format = 'pdf'): Observable<Blob> {
    let params = new HttpParams()
      .set('periodStart', periodStart)
      .set('periodEnd', periodEnd)
      .set('format', format);
    if (region) params = params.set('region', region);
    return this.http.get(`${this.baseUrl}/reports/churn/export`, { params, responseType: 'blob' });
  }

  exportNetworkUtilisation(cycleId: number, region?: string, format = 'pdf'): Observable<Blob> {
    let params = new HttpParams()
      .set('cycleId', cycleId.toString())
      .set('format', format);
    if (region) params = params.set('region', region);
    return this.http.get(`${this.baseUrl}/reports/network-utilisation/export`, { params, responseType: 'blob' });
  }

  exportSLACompliance(periodStart: string, periodEnd: string, format = 'pdf'): Observable<Blob> {
    let params = new HttpParams()
      .set('periodStart', periodStart)
      .set('periodEnd', periodEnd)
      .set('format', format);
    return this.http.get(`${this.baseUrl}/reports/sla-compliance/export`, { params, responseType: 'blob' });
  }

  exportCollectionEfficiency(cycleId: number, format = 'pdf'): Observable<Blob> {
    let params = new HttpParams().set('cycleId', cycleId.toString()).set('format', format);
    return this.http.get(`${this.baseUrl}/reports/collection-efficiency/export`, { params, responseType: 'blob' });
  }

  exportSubscriberGrowth(periodStart: string, periodEnd: string, format = 'pdf'): Observable<Blob> {
    let params = new HttpParams()
      .set('periodStart', periodStart)
      .set('periodEnd', periodEnd)
      .set('format', format);
    return this.http.get(`${this.baseUrl}/reports/subscriber-growth/export`, { params, responseType: 'blob' });
  }
}
