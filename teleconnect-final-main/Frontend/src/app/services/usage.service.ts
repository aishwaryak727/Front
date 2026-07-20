import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UsageRecordRequest {
  lineId: number;
  billingCycleId: number;
  usageType: string; // DATA, VOICE, SMS
  quantity: number;
  usageDate: string; // YYYY-MM-DDTHH:MM:SS
  dataLimitMb: number;
  voiceLimitMin: number;
  smsLimit: number;
}

export interface UsageRecordResponse {
  recordId: number;
  lineId: number;
  billingCycleId: number;
  usageType: string;
  quantity: number;
  unit: string;
  usageDate: string;
}

export interface UsageSummaryResponse {
  summaryId: number;
  lineId: number;
  billingCycleId: number;
  dataUsedMb: number;
  voiceUsedMin: number;
  smsUsed: number;
  dataRemainingMb: number;
  voiceRemainingMin: number;
  smsRemaining: number;
  lastUpdated: string;
}

export interface LimitStatusResponse {
  lineId: number;
  billingCycleId: number;
  dataLimitMb: number;
  dataUsedMb: number;
  dataPercentage: number;
  voiceLimitMin: number;
  voiceUsedMin: number;
  voicePercentage: number;
  smsLimit: number;
  smsUsed: number;
  smsPercentage: number;
}

export interface AlertResponse {
  lineId: number;
  billingCycleId: number;
  dataAlert: string; // OK, WARNING, CRITICAL
  voiceAlert: string;
  smsAlert: string;
}

export interface AnalyticsTrendResponse {
  lineId: number;
  totalRecordsCount: number;
  averageDataUsedMb: number;
  averageVoiceUsedMin: number;
  averageSmsUsed: number;
  cyclesTrend: Array<{
    billingCycleId: number;
    totalUsageRecords: number;
    totalDataUsedMb: number;
    totalVoiceUsedMin: number;
    totalSmsUsed: number;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class UsageService {
  private readonly baseUrl = 'http://localhost:9090/teleConnect/usage';

  constructor(private http: HttpClient) {}

  // Usage Recording
  createRecord(req: UsageRecordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/createRecord`, req);
  }

  fetchRecords(lineId: number): Observable<{ lineId: number; records: UsageRecordResponse[] }> {
    return this.http.get<{ lineId: number; records: UsageRecordResponse[] }>(`${this.baseUrl}/fetchRecords/${lineId}`);
  }

  fetchRecordsByCycle(lineId: number, billingCycleId: number): Observable<{ lineId: number; billingCycleId: number; records: UsageRecordResponse[] }> {
    return this.http.get<{ lineId: number; billingCycleId: number; records: UsageRecordResponse[] }>(`${this.baseUrl}/fetchRecords/${lineId}/${billingCycleId}`);
  }

  // Usage Summary
  fetchSummary(lineId: number, billingCycleId: number): Observable<UsageSummaryResponse> {
    return this.http.get<UsageSummaryResponse>(`${this.baseUrl}/fetchSummary/${lineId}/${billingCycleId}`);
  }

  updateSummary(lineId: number, billingCycleId: number, body: { dataUsedMb?: number; voiceUsedMin?: number; smsUsed?: number }): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/updateSummary/${lineId}/${billingCycleId}`, body);
  }

  // Plan Limit Tracking
  getLimitStatus(lineId: number, billingCycleId: number, dataLimitMb: number, voiceLimitMin: number, smsLimit: number): Observable<LimitStatusResponse> {
    let params = new HttpParams()
      .set('dataLimitMb', dataLimitMb.toString())
      .set('voiceLimitMin', voiceLimitMin.toString())
      .set('smsLimit', smsLimit.toString());
    return this.http.get<LimitStatusResponse>(`${this.baseUrl}/limitStatus/${lineId}/${billingCycleId}`, { params });
  }

  getRemaining(lineId: number, billingCycleId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/remaining/${lineId}/${billingCycleId}`);
  }

  // Alerts
  getAlerts(lineId: number, billingCycleId: number, dataLimitMb: number, voiceLimitMin: number, smsLimit: number): Observable<AlertResponse> {
    let params = new HttpParams()
      .set('dataLimitMb', dataLimitMb.toString())
      .set('voiceLimitMin', voiceLimitMin.toString())
      .set('smsLimit', smsLimit.toString());
    return this.http.get<AlertResponse>(`${this.baseUrl}/alerts/${lineId}/${billingCycleId}`, { params });
  }

  // Analytics
  getAnalyticsTrend(lineId: number): Observable<AnalyticsTrendResponse> {
    return this.http.get<AnalyticsTrendResponse>(`${this.baseUrl}/analytics/${lineId}`);
  }

  getTopUsage(lineId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/analytics/${lineId}/top-usage`);
  }
}
