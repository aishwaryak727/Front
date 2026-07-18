import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface UserResponse {
  userId: number;
  email: string;
  name: string;
  phone: string;
  roleName: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly baseUrl = 'http://localhost:9090/teleConnect/iam/api/users';

  constructor(private http: HttpClient) {}

  getMe(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/me`);
  }

  getUserById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/${id}`);
  }

  updateUser(id: number, req: { name: string; phone: string; email?: string }): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/${id}`, req);
  }

  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.baseUrl);
  }

  searchUsers(filters: { name?: string; email?: string; phone?: string; status?: string; role?: string }): Observable<UserResponse[]> {
    let params = new HttpParams();
    if (filters.name) params = params.set('name', filters.name);
    if (filters.email) params = params.set('email', filters.email);
    if (filters.phone) params = params.set('phone', filters.phone);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.role) params = params.set('role', filters.role);

    return this.http.get<UserResponse[]>(`${this.baseUrl}/search`, { params });
  }
}
