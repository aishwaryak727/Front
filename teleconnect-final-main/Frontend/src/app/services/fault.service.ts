import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FaultTicketRequest {
  accountId: number;
  lineId: number;
  faultType: string; // NETWORK, DEVICE, BILLING, SIMCARD, OTHER
  description: string;
  priority?: string; // L, M, H
  raisedDate: string; // LocalDate
  resolvedDate?: string; // LocalDate
  assignedToId?: number;
  status?: string; // OPEN, ASSIGNED, RESOLVED
}

export interface FaultTicketResponse {
  ticketId: number;
  accountId: number;
  lineId: number;
  faultType: string;
  description: string;
  priority: string;
  raisedDate: string;
  resolvedDate: string | null;
  assignedToId: number | null;
  status: string;
}

export interface ServiceRequestRequest {
  accountId: number;
  lineId: number;
  requestType: string; // SIMSwap, Porting, Activation, Cancellation, Other
  requestedBy: number; // userId
  raisedDate: string; // LocalDate
  status?: string; // O (Open), C (Completed), X (Canceled)
}

export interface ServiceRequestResponse {
  requestId: number;
  accountId: number;
  lineId: number;
  requestType: string;
  requestedBy: number;
  raisedDate: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class FaultService {
  private readonly baseUrl = 'http://localhost:9090/teleConnect/fault';

  constructor(private http: HttpClient) {}

  // Fault Tickets
  createTicket(req: FaultTicketRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/createTickets`, req);
  }

  getAllTickets(): Observable<FaultTicketResponse[]> {
    return this.http.get<FaultTicketResponse[]>(`${this.baseUrl}/getAllTickets`);
  }

  getTicketById(ticketId: number): Observable<FaultTicketResponse> {
    return this.http.get<FaultTicketResponse>(`${this.baseUrl}/getTickets/${ticketId}`);
  }

  assignTicket(ticketId: number, req: FaultTicketRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/assignTickets/${ticketId}`, req);
  }

  updateTicket(ticketId: number, req: FaultTicketRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/updateTickets/${ticketId}`, req);
  }

  resolveTicket(ticketId: number, req: FaultTicketRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/resolveTickets/${ticketId}`, req);
  }

  // Service Requests
  createRequest(req: ServiceRequestRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/createRequests`, req);
  }

  getAllRequests(): Observable<ServiceRequestResponse[]> {
    return this.http.get<ServiceRequestResponse[]>(`${this.baseUrl}/getAllRequests`);
  }

  getRequestById(requestId: number): Observable<ServiceRequestResponse> {
    return this.http.get<ServiceRequestResponse>(`${this.baseUrl}/getRequests/${requestId}`);
  }

  updateRequest(requestId: number, req: ServiceRequestRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/updateRequests/${requestId}`, req);
  }

  cancelRequest(requestId: number): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/cancelRequests/${requestId}`, {});
  }
}
