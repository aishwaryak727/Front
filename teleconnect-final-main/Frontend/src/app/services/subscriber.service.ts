import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AccountResponse {
  accountId: number;
  subscriberId: number;
  accountType: string;
  registrationDate: string;
  kycStatus: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface AccountListResponse {
  subscribers: AccountResponse[];
  totalCount: number;
}

export interface SimLineResponse {
  lineId: number;
  accountId: number;
  msisdn: string;
  iccid: string;
  activationDate: string;
  serviceType: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class SubscriberService {
  private readonly baseUrl = 'http://localhost:9090/teleConnect/api/subscribers';

  constructor(private http: HttpClient) {}

  // Account management APIs
  createAccount(req: { subscriberId: number; accountType: string; kycStatus?: string }): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(this.baseUrl, req);
  }

  getAccount(accountId: number): Observable<AccountResponse> {
    return this.http.get<AccountResponse>(`${this.baseUrl}/${accountId}`);
  }

  getAllAccounts(status?: string, subscriberId?: number): Observable<AccountListResponse> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (subscriberId) params = params.set('subscriberId', subscriberId.toString());

    return this.http.get<AccountListResponse>(this.baseUrl, { params });
  }

  getExpiredKyc(): Observable<AccountResponse[]> {
    return this.http.get<AccountResponse[]>(`${this.baseUrl}/kyc/expired`);
  }

  updateKyc(accountId: number, kycStatus: string): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/${accountId}/kyc`, { kycStatus });
  }

  updateStatus(accountId: number, status: string): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/${accountId}/status`, { status });
  }

  deleteAccount(accountId: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/${accountId}`);
  }

  // SIM Line management APIs
  createSimLine(accountId: number, req: { msisdn: string; iccid: string; serviceType?: string }): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/${accountId}/simLines`, req);
  }

  getSimLines(accountId: number, status?: string): Observable<SimLineResponse[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<SimLineResponse[]>(`${this.baseUrl}/${accountId}/simLines`, { params });
  }

  getSimLine(accountId: number, lineId: number): Observable<SimLineResponse> {
    return this.http.get<SimLineResponse>(`${this.baseUrl}/${accountId}/simLines/${lineId}`);
  }

  lookupByMsisdn(msisdn: string): Observable<SimLineResponse> {
    let params = new HttpParams().set('msisdn', msisdn);
    return this.http.get<SimLineResponse>(`${this.baseUrl}/sim-lines/lookup`, { params });
  }

  updateSimStatus(accountId: number, lineId: number, status: string): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/${accountId}/simLines/${lineId}/status`, { status });
  }

  replaceSim(accountId: number, lineId: number, newIccid: string): Observable<SimLineResponse> {
    return this.http.put<SimLineResponse>(`${this.baseUrl}/${accountId}/simLines/${lineId}/replace`, { newIccid });
  }

  updateServiceType(accountId: number, lineId: number, serviceType: string): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/${accountId}/simLines/${lineId}/service-type`, { serviceType });
  }

  deleteSimLine(accountId: number, lineId: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/${accountId}/simLines/${lineId}`);
  }
}
