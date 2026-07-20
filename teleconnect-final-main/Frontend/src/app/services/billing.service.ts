import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BillingCycleRequest {
  accountId: number;
  cycleStart: string; // LocalDate
  cycleEnd: string; // LocalDate
  status: string; // OPEN, CLOSED, INVOICED
}

export interface BillingCycleResponse {
  cycleId: number;
  accountId: number;
  cycleStart: string;
  cycleEnd: string;
  status: string;
}

export interface CycleGenerationRequest {
  cycleDate: string; // LocalDate
  dryRun: boolean;
}

export interface InvoiceResponse {
  invoiceId: number;
  accountId: number;
  billingCycleId: number;
  amountDue: number;
  taxAmount: number;
  lateFee: number;
  discountAmount: number;
  totalAmount: number;
  amountPaid: number;
  invoiceDate: string;
  dueDate: string;
  status: string; // DRAFT, SENT, PAID, OVERDUE, DISPUTED
  createdAt: string;
  updatedAt: string;
}

export interface InvoiceGenerationRequest {
  accountId: number;
  billingCycleId: number;
}

export interface PaymentRequest {
  amountPaid: number;
  paymentMethod: string;
  transactionRef: string;
}

export interface PaymentResponse {
  paymentId: number;
  invoiceId: number;
  amountPaid: number;
  paymentDate: string;
  paymentMethod: string;
  transactionRef: string;
}

export interface LateFeeRequest {
  feeAmount: number;
  reason: string;
}

export interface LateFeeWaiverRequest {
  waiverReason: string;
  authorisedBy: string;
}

export interface DisputeRequest {
  invoiceId: number;
  disputeReason: string;
  disputedAmount: number;
  description: string;
}

export interface DisputeResponse {
  disputeId: number;
  invoiceId: number;
  subscriberId: number;
  disputeReason: string;
  disputedAmount: number;
  description: string;
  status: string; // OPEN, UNDER_REVIEW, RESOLVED, REJECTED
  assignedTo: string;
  notes: string;
  resolution: string;
  creditAmount: number;
  resolutionNotes: string;
  createdAt: string;
  updatedAt: string;
}

export interface DisputeReviewRequest {
  assignedTo: string;
  notes: string;
}

export interface DisputeResolveRequest {
  resolution: string;
  creditAmount: number;
  resolutionNotes: string;
}

export interface OverdueReportResponse {
  totalOverdueAmount: number;
  totalOverdueInvoices: number;
  agingBuckets: { [key: string]: number }; // e.g. "0-30": 1500, "31-60": 500
  overdueInvoices: InvoiceResponse[];
}

export interface CollectionReportResponse {
  totalInvoiced: number;
  totalCollected: number;
  collectionEfficiency: number; // percentage
  periodStart: string;
  periodEnd: string;
  region: string;
}

export interface DisputeSummaryResponse {
  totalDisputes: number;
  resolvedDisputes: number;
  openDisputes: number;
  averageResolutionTimeHours: number;
  slaCompliancePercentage: number;
}

@Injectable({
  providedIn: 'root'
})
export class BillingService {
  private readonly baseUrl = 'http://localhost:9090/teleConnect/billing';

  constructor(private http: HttpClient) {}

  // Billing Cycles
  createBillingCycle(req: BillingCycleRequest): Observable<BillingCycleResponse> {
    return this.http.post<BillingCycleResponse>(`${this.baseUrl}/cycles`, req);
  }

  generateInvoicesBatch(req: CycleGenerationRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/cycles/generate`, req);
  }

  getCyclesByAccount(accountId: number, status?: string, page = 0, size = 5): Observable<BillingCycleResponse[]> {
    let params = new HttpParams().set('page', page.toString()).set('size', size.toString());
    if (status) params = params.set('status', status);
    return this.http.get<BillingCycleResponse[]>(`${this.baseUrl}/cycles/account/${accountId}`, { params });
  }

  closeCycle(cycleId: number): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/cycles/${cycleId}/close`, {});
  }

  updateCycleStatus(cycleId: number, status: string): Observable<BillingCycleResponse> {
    let params = new HttpParams().set('status', status);
    return this.http.put<BillingCycleResponse>(`${this.baseUrl}/cycles/${cycleId}/status`, {}, { params });
  }

  getBillingCycle(cycleId: number): Observable<BillingCycleResponse> {
    return this.http.get<BillingCycleResponse>(`${this.baseUrl}/cycles/${cycleId}`);
  }

  // Invoices
  generateInvoice(req: InvoiceGenerationRequest): Observable<InvoiceResponse> {
    return this.http.post<InvoiceResponse>(`${this.baseUrl}/invoices/generate`, req);
  }

  getInvoicesByAccount(accountId: number, status?: string, fromDate?: string, toDate?: string): Observable<InvoiceResponse[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    if (fromDate) params = params.set('fromDate', fromDate);
    if (toDate) params = params.set('toDate', toDate);
    return this.http.get<InvoiceResponse[]>(`${this.baseUrl}/invoices/account/${accountId}`, { params });
  }

  getInvoicesByStatus(status: string): Observable<InvoiceResponse[]> {
    return this.http.get<InvoiceResponse[]>(`${this.baseUrl}/invoices/status/${status}`);
  }

  markOverdue(): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/invoices/mark-overdue`, {});
  }

  getInvoice(invoiceId: number): Observable<InvoiceResponse> {
    return this.http.get<InvoiceResponse>(`${this.baseUrl}/invoices/${invoiceId}`);
  }

  sendInvoice(invoiceId: number): Observable<InvoiceResponse> {
    return this.http.put<InvoiceResponse>(`${this.baseUrl}/invoices/${invoiceId}/send`, {});
  }

  payInvoice(invoiceId: number, req: PaymentRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/invoices/${invoiceId}/pay`, req);
  }

  applyLateFee(invoiceId: number, req: LateFeeRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/invoices/${invoiceId}/latefee`, req);
  }

  waiveLateFee(invoiceId: number, req: LateFeeWaiverRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/invoices/${invoiceId}/latefee/waive`, req);
  }

  downloadInvoicePdf(invoiceId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/invoices/${invoiceId}/download`, { responseType: 'blob' });
  }

  downloadAccountStatementPdf(accountId: number): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/invoices/account/${accountId}/statement`, { responseType: 'blob' });
  }

  // Payments
  recordPayment(req: PaymentRequest & { invoiceId: number }): Observable<PaymentResponse> {
    return this.http.post<PaymentResponse>(`${this.baseUrl}/payments`, req);
  }

  getPayment(paymentId: number): Observable<PaymentResponse> {
    return this.http.get<PaymentResponse>(`${this.baseUrl}/payments/${paymentId}`);
  }

  getPaymentsByInvoice(invoiceId: number): Observable<PaymentResponse[]> {
    return this.http.get<PaymentResponse[]>(`${this.baseUrl}/payments/invoice/${invoiceId}`);
  }

  // Disputes
  raiseDispute(req: DisputeRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/disputes`, req);
  }

  getDisputesByAccount(accountId: number, status?: string): Observable<DisputeResponse[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<DisputeResponse[]>(`${this.baseUrl}/disputes/account/${accountId}`, { params });
  }

  getDisputesByInvoice(invoiceId: number): Observable<DisputeResponse[]> {
    return this.http.get<DisputeResponse[]>(`${this.baseUrl}/disputes/invoice/${invoiceId}`);
  }

  getDisputesBySubscriber(subscriberId: number): Observable<DisputeResponse[]> {
    return this.http.get<DisputeResponse[]>(`${this.baseUrl}/disputes/subscriber/${subscriberId}`);
  }

  getDisputesByStatus(status: string): Observable<DisputeResponse[]> {
    return this.http.get<DisputeResponse[]>(`${this.baseUrl}/disputes/status/${status}`);
  }

  getDispute(disputeId: number): Observable<DisputeResponse> {
    return this.http.get<DisputeResponse>(`${this.baseUrl}/disputes/${disputeId}`);
  }

  reviewDispute(disputeId: number, req: DisputeReviewRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/disputes/${disputeId}/review`, req);
  }

  resolveDispute(disputeId: number, req: DisputeResolveRequest): Observable<DisputeResponse> {
    return this.http.put<DisputeResponse>(`${this.baseUrl}/disputes/${disputeId}/resolve`, req);
  }

  updateDisputeStatus(disputeId: number, status: string): Observable<DisputeResponse> {
    let params = new HttpParams().set('status', status);
    return this.http.put<DisputeResponse>(`${this.baseUrl}/disputes/${disputeId}/status`, {}, { params });
  }

  // Reports
  getOverdueReport(region?: string, agingBucket?: string): Observable<OverdueReportResponse> {
    let params = new HttpParams();
    if (region) params = params.set('region', region);
    if (agingBucket) params = params.set('agingBucket', agingBucket);
    return this.http.get<OverdueReportResponse>(`${this.baseUrl}/reports/overdue`, { params });
  }

  getCollectionReport(fromDate: string, toDate: string, region?: string): Observable<CollectionReportResponse> {
    let params = new HttpParams().set('fromDate', fromDate).set('toDate', toDate);
    if (region) params = params.set('region', region);
    return this.http.get<CollectionReportResponse>(`${this.baseUrl}/reports/collection`, { params });
  }

  getDisputeSummaryReport(fromDate: string, toDate: string): Observable<DisputeSummaryResponse> {
    let params = new HttpParams().set('fromDate', fromDate).set('toDate', toDate);
    return this.http.get<DisputeSummaryResponse>(`${this.baseUrl}/reports/disputes/summary`, { params });
  }
}
