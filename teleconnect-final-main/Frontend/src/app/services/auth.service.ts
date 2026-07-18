import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface LoginResponse {
  token: string;
  role: string;
  name: string;
  mustChangePassword: boolean;
  permissions: string[];
}

export interface UserSession {
  token: string;
  role: string;
  name: string;
  permissions: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = 'http://localhost:9090/teleConnect/iam/api/auth';
  
  // Signals for state management
  readonly currentUser = signal<UserSession | null>(null);
  readonly isAuthenticated = computed(() => this.currentUser() !== null);
  readonly userRole = computed(() => this.currentUser()?.role || '');
  readonly userPermissions = computed(() => this.currentUser()?.permissions || []);

  constructor(private http: HttpClient) {
    this.loadSession();
  }

  private loadSession() {
    const sessionData = localStorage.getItem('tc_session');
    if (sessionData) {
      try {
        this.currentUser.set(JSON.parse(sessionData));
      } catch (e) {
        this.logoutLocal();
      }
    }
  }

  register(req: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/register`, req);
  }

  login(req: any): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/login`, req).pipe(
      tap(res => {
        const session: UserSession = {
          token: res.token,
          role: res.role,
          name: res.name,
          permissions: res.permissions || []
        };
        localStorage.setItem('tc_session', JSON.stringify(session));
        this.currentUser.set(session);
      })
    );
  }

  logout(): Observable<any> {
    // Call backend logout first, then clear locally
    return this.http.post(`${this.baseUrl}/logout`, {}).pipe(
      tap({
        next: () => this.logoutLocal(),
        error: () => this.logoutLocal() // fallback if server call fails
      })
    );
  }

  logoutLocal() {
    localStorage.removeItem('tc_session');
    this.currentUser.set(null);
  }

  changePassword(req: any): Observable<any> {
    return this.http.put(`${this.baseUrl}/changePassword`, req);
  }

  hasPermission(permission: string): boolean {
    return this.userPermissions().includes(permission);
  }

  hasAnyPermission(permissions: string[]): boolean {
    return permissions.some(p => this.hasPermission(p));
  }
}
