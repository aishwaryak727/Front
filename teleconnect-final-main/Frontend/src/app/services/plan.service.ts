import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TelecomPlanRequest {
  name: string;
  type: string;
  dataGb: number;
  voiceMinutes: number;
  smsCount: number;
  validityDays: number;
  planPrice: number;
  status?: string;
}

export interface TelecomPlanResponse {
  planId: number;
  name: string;
  type: string;
  dataGb: number;
  voiceMinutes: number;
  smsCount: number;
  validityDays: number;
  planPrice: number;
  status: string;
}

export interface AddOnRequest {
  name: string;
  type: string;
  quota: number;
  validityDays: number;
  price: number;
}

export interface AddOnResponse {
  addOnId: number;
  name: string;
  type: string;
  quota: number;
  validityDays: number;
  price: number;
  status: string;
}

export interface ServiceSubscriptionRequest {
  lineId?: number;
  planId?: number;
  addOnId?: number;
  activationDate?: string;
  expiryDate?: string;
  renewalType?: string;
  status?: string;
}

export interface ServiceSubscriptionResponse {
  subscriptionId: number;
  lineId: number;
  planId: number | null;
  addOnId: number | null;
  activationDate: string;
  expiryDate: string;
  renewalType: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class PlanService {
  private readonly baseUrl = 'http://localhost:9090/teleConnect/plan';

  constructor(private http: HttpClient) {}

  // Telecom Plans
  createPlan(req: TelecomPlanRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/createPlans`, req);
  }

  getAllPlans(): Observable<TelecomPlanResponse[]> {
    return this.http.get<TelecomPlanResponse[]>(`${this.baseUrl}/getAllPlans`);
  }

  getPlanById(planId: number): Observable<TelecomPlanResponse> {
    return this.http.get<TelecomPlanResponse>(`${this.baseUrl}/getPlans/${planId}`);
  }

  updatePlan(planId: number, req: TelecomPlanRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/updatePlans/${planId}`, req);
  }

  // Add-Ons
  createAddOn(req: AddOnRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/createAddOns`, req);
  }

  getAllAddOns(): Observable<AddOnResponse[]> {
    return this.http.get<AddOnResponse[]>(`${this.baseUrl}/getAllAddOns`);
  }

  getAddOnById(addOnId: number): Observable<AddOnResponse> {
    return this.http.get<AddOnResponse>(`${this.baseUrl}/getAddOns/${addOnId}`);
  }

  // Subscriptions
  createSubscription(req: ServiceSubscriptionRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.baseUrl}/createSubscriptions`, req);
  }

  getAllSubscriptions(): Observable<ServiceSubscriptionResponse[]> {
    return this.http.get<ServiceSubscriptionResponse[]>(`${this.baseUrl}/getAllSubscriptions`);
  }

  getSubscriptionById(subscriptionId: number): Observable<ServiceSubscriptionResponse> {
    return this.http.get<ServiceSubscriptionResponse>(`${this.baseUrl}/getSubscriptions/${subscriptionId}`);
  }

  updateSubscription(subscriptionId: number, req: ServiceSubscriptionRequest): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/updateSubscriptions/${subscriptionId}`, req);
  }
}
