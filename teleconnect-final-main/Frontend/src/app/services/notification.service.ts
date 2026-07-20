import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface NotificationRequest {
  userId: number;
  message: string;
  category: string; // USAGE, BILLING, FAULT, PLAN, COMPLIANCE
}

export interface StatusUpdateRequest {
  status: string; // READ, DISMISSED
}

export interface NotificationResponse {
  notificationId: number;
  userId: number;
  message: string;
  category: string;
  status: string; // UNREAD, READ, DISMISSED
  createdDate: string;
}

export interface MessageResponse {
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private readonly baseUrl = 'http://localhost:9090/teleConnect/notification';

  constructor(private http: HttpClient) {}

  createNotification(req: NotificationRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.baseUrl}/createNotification`, req);
  }

  getNotifications(userId: number): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>(`${this.baseUrl}/fetchNotifications/${userId}`);
  }

  getNotificationById(notificationId: number): Observable<NotificationResponse> {
    return this.http.get<NotificationResponse>(`${this.baseUrl}/fetchNotificationById/${notificationId}`);
  }

  getByStatus(userId: number, status: string): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>(`${this.baseUrl}/fetchByStatus/${userId}/${status}`);
  }

  getByCategory(userId: number, category: string): Observable<NotificationResponse[]> {
    return this.http.get<NotificationResponse[]>(`${this.baseUrl}/fetchByCategory/${userId}/${category}`);
  }

  getUnreadCount(userId: number): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/unreadCount/${userId}`);
  }

  markAsRead(notificationId: number): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.baseUrl}/markAsRead/${notificationId}`, {});
  }

  dismissNotification(notificationId: number): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.baseUrl}/dismiss/${notificationId}`, {});
  }

  markAllAsRead(userId: number): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${this.baseUrl}/markAllAsRead/${userId}`, {});
  }
}
