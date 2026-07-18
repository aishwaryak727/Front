import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Role {
  roleId: number;
  roleName: string;
  permissions: {
    permissionId: number;
    permissionName: string;
    description: string;
  }[];
}

export interface AuditLogResponse {
  auditId: number;
  userId: number;
  action: string;
  module: string;
  ipAddress: string;
  timestamp: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly baseUrl = 'http://localhost:9090/teleConnect/iam/api';

  constructor(private http: HttpClient) {}

  createStaff(req: { email: string; name: string; phone: string; roleName: string }): Observable<any> {
    return this.http.post(`${this.baseUrl}/admin/users/createStaff`, req);
  }

  updateStatus(userId: number, status: string): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/users/${userId}/status`, { status });
  }

  resetPassword(userId: number): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/admin/users/${userId}/resetPassword`, {});
  }

  getRoles(): Observable<Role[]> {
    return this.http.get<Role[]>(`${this.baseUrl}/roles`);
  }

  getRolePermissions(roleId: number): Observable<Role> {
    return this.http.get<Role>(`${this.baseUrl}/roles/${roleId}/permissions`);
  }

  getAuditLogs(filters: { action?: string; module?: string; page?: number; size?: number }): Observable<PageResponse<AuditLogResponse>> {
    let params = new HttpParams();
    if (filters.action) params = params.set('action', filters.action);
    if (filters.module) params = params.set('module', filters.module);
    if (filters.page !== undefined) params = params.set('page', filters.page.toString());
    if (filters.size !== undefined) params = params.set('size', filters.size.toString());

    return this.http.get<PageResponse<AuditLogResponse>>(`${this.baseUrl}/auditLogs`, { params });
  }

  getLogsByUser(userId: number, filters: { action?: string; module?: string; page?: number; size?: number }): Observable<PageResponse<AuditLogResponse>> {
    let params = new HttpParams();
    if (filters.action) params = params.set('action', filters.action);
    if (filters.module) params = params.set('module', filters.module);
    if (filters.page !== undefined) params = params.set('page', filters.page.toString());
    if (filters.size !== undefined) params = params.set('size', filters.size.toString());

    return this.http.get<PageResponse<AuditLogResponse>>(`${this.baseUrl}/auditLogs/user/${userId}`, { params });
  }

  recordAudit(action: string, module: string, ipAddress?: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auditLogs`, { action, module, ipAddress });
  }
}
