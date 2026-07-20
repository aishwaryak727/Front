import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { NotificationService, NotificationResponse } from '../services/notification.service';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-notifications-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="notifications-container animate-fade-in">
      <header class="notifications-header">
        <h1>Support Notifications</h1>
        <p>Stay up to date with subscription changes, billing reminders, quota alerts, and support updates</p>
      </header>

      <!-- Main Layout -->
      <div class="inbox-layout">
        <!-- Notifications List & Filters -->
        <div class="glass-card inbox-panel">
          <div class="inbox-header mb-16">
            <h3>Notification Inbox</h3>
            <div class="header-actions">
              <span class="unread-count-badge" *ngIf="unreadCount > 0">
                {{ unreadCount }} Unread
              </span>
              <button (click)="markAllAsRead()" *ngIf="unreadCount > 0" class="btn btn-secondary btn-small">
                ✓ Mark All As Read
              </button>
            </div>
          </div>

          <!-- Category and Status filter buttons -->
          <div class="filters-row mb-16">
            <div class="filter-group">
              <span class="filter-label">Status:</span>
              <button [class.active]="statusFilter === 'ALL'" (click)="setStatusFilter('ALL')" class="btn-filter">All</button>
              <button [class.active]="statusFilter === 'UNREAD'" (click)="setStatusFilter('UNREAD')" class="btn-filter">Unread</button>
              <button [class.active]="statusFilter === 'READ'" (click)="setStatusFilter('READ')" class="btn-filter">Read</button>
            </div>

            <div class="filter-group mt-8">
              <span class="filter-label">Category:</span>
              <button [class.active]="categoryFilter === 'ALL'" (click)="setCategoryFilter('ALL')" class="btn-filter">All Categories</button>
              <button [class.active]="categoryFilter === 'USAGE'" (click)="setCategoryFilter('USAGE')" class="btn-filter">Usage</button>
              <button [class.active]="categoryFilter === 'BILLING'" (click)="setCategoryFilter('BILLING')" class="btn-filter">Billing</button>
              <button [class.active]="categoryFilter === 'FAULT'" (click)="setCategoryFilter('FAULT')" class="btn-filter">Support</button>
              <button [class.active]="categoryFilter === 'PLAN'" (click)="setCategoryFilter('PLAN')" class="btn-filter">Plans</button>
            </div>
          </div>

          <!-- Alert message -->
          <div *ngIf="alertMessage" class="alert alert-info mt-12 mb-16">
            ℹ️ {{ alertMessage }}
          </div>
          <div *ngIf="errorMessage" class="alert alert-danger mt-12 mb-16">
            ⚠️ {{ errorMessage }}
          </div>

          <!-- List of Notification messages -->
          <div class="notifications-list">
            <div *ngFor="let n of filteredNotifications" class="notification-card" [class.unread]="n.status === 'UNREAD'">
              <div class="card-left">
                <span class="category-icon">{{ getCategoryIcon(n.category) }}</span>
              </div>
              <div class="card-body">
                <div class="card-meta">
                  <span class="category-name">{{ n.category }}</span>
                  <span class="timestamp">{{ n.createdDate | date:'short' }}</span>
                </div>
                <p class="message-text">{{ n.message }}</p>
              </div>
              <div class="card-actions">
                <!-- Mark Single Read -->
                <button *ngIf="n.status === 'UNREAD'" (click)="markAsRead(n.notificationId)" class="action-btn" title="Mark as read">
                  ✓
                </button>
                <!-- Dismiss notification -->
                <button *ngIf="n.status !== 'DISMISSED'" (click)="dismissNotification(n.notificationId)" class="action-btn dismiss" title="Dismiss">
                  ✕
                </button>
              </div>
            </div>

            <div *ngIf="filteredNotifications.length === 0" class="text-center muted-text py-40">
              No notifications found matching the selected filters.
            </div>
          </div>
        </div>

        <!-- Create & Dispatch notifications (CS Agent / Admin) -->
        <div class="glass-card dispatch-panel" *ngIf="authService.hasPermission('CREATE_NOTIFICATION')">
          <h3>Create & Send Notification</h3>
          <p class="description mb-16">Dispatch custom SMS/dashboard alert to subscriber user account.</p>

          <form [formGroup]="createForm" (ngSubmit)="onSubmitNotification()" class="mt-16">
            <div class="form-group">
              <label class="form-label">Subscriber User ID</label>
              <input type="number" formControlName="userId" class="form-control" placeholder="e.g. 1">
            </div>

            <div class="form-group">
              <label class="form-label">Category</label>
              <select formControlName="category" class="form-control form-select">
                <option value="USAGE">USAGE</option>
                <option value="BILLING">BILLING</option>
                <option value="FAULT">FAULT</option>
                <option value="PLAN">PLAN</option>
                <option value="COMPLIANCE">COMPLIANCE</option>
              </select>
            </div>

            <div class="form-group">
              <label class="form-label">Message Details</label>
              <textarea formControlName="message" class="form-control" rows="4" placeholder="Enter alert message details..."></textarea>
            </div>

            <button type="submit" [disabled]="createForm.invalid" class="btn btn-primary btn-block mt-12">
              Dispatch Notification
            </button>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .notifications-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .notifications-header h1 {
      font-size: 2.2rem;
      margin-bottom: 6px;
    }
    .notifications-header p {
      color: var(--text-secondary);
      font-size: 1rem;
    }
    .inbox-layout {
      display: grid;
      grid-template-columns: 1fr 340px;
      gap: 24px;
    }
    @media (max-width: 992px) {
      .inbox-layout {
        grid-template-columns: 1fr;
      }
    }
    .inbox-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .unread-count-badge {
      background: var(--accent-glow);
      color: var(--accent-primary);
      padding: 4px 10px;
      border-radius: 999px;
      font-size: 0.85rem;
      font-weight: 700;
      border: 1px solid var(--accent-primary);
    }
    .filters-row {
      display: flex;
      flex-direction: column;
      gap: 12px;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 16px;
    }
    .filter-group {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }
    .filter-label {
      font-size: 0.85rem;
      color: var(--text-secondary);
      font-weight: 600;
      min-width: 70px;
    }
    .btn-filter {
      background: transparent;
      border: 1px solid var(--border-color);
      color: var(--text-secondary);
      padding: 4px 12px;
      border-radius: var(--radius-sm);
      cursor: pointer;
      font-size: 0.85rem;
      transition: var(--transition-smooth);
    }
    .btn-filter:hover {
      color: white;
      border-color: var(--text-secondary);
    }
    .btn-filter.active {
      background: var(--accent-primary);
      border-color: var(--accent-primary);
      color: var(--bg-primary);
      font-weight: 700;
    }
    .notifications-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .notification-card {
      display: flex;
      gap: 16px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 16px;
      transition: var(--transition-smooth);
      position: relative;
    }
    .notification-card.unread {
      border-left: 4px solid var(--accent-primary);
      background: rgba(255, 255, 255, 0.01);
    }
    .category-icon {
      font-size: 1.5rem;
    }
    .card-body {
      flex: 1;
    }
    .card-meta {
      display: flex;
      justify-content: space-between;
      font-size: 0.8rem;
      color: var(--text-secondary);
      margin-bottom: 4px;
    }
    .category-name {
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .message-text {
      color: var(--text-primary);
      line-height: 1.4;
    }
    .card-actions {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .action-btn {
      background: rgba(255, 255, 255, 0.03);
      border: 1px solid var(--border-color);
      color: var(--text-secondary);
      width: 28px;
      height: 28px;
      border-radius: 50%;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.85rem;
      transition: var(--transition-smooth);
    }
    .action-btn:hover {
      color: var(--success);
      border-color: var(--success);
      background: rgba(46, 213, 115, 0.05);
    }
    .action-btn.dismiss:hover {
      color: var(--danger);
      border-color: var(--danger);
      background: rgba(255, 71, 87, 0.05);
    }
  `]
})
export class NotificationsDashboardComponent implements OnInit {
  private readonly notificationService = inject(NotificationService);
  private readonly userService = inject(UserService);
  readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  notifications: NotificationResponse[] = [];
  filteredNotifications: NotificationResponse[] = [];

  currentUserId!: number;
  unreadCount = 0;

  // Filters
  statusFilter = 'ALL';
  categoryFilter = 'ALL';

  // Forms
  createForm!: FormGroup;

  // Alerts
  alertMessage = '';
  errorMessage = '';

  ngOnInit() {
    this.initForms();
    this.loadUserDataAndNotifications();
  }

  private initForms() {
    this.createForm = this.fb.group({
      userId: ['', [Validators.required, Validators.min(1)]],
      category: ['USAGE', Validators.required],
      message: ['', [Validators.required, Validators.minLength(5)]]
    });
  }

  private loadUserDataAndNotifications() {
    this.userService.getMe().subscribe({
      next: (user) => {
        this.currentUserId = user.userId;
        this.loadNotifications();
      }
    });
  }

  loadNotifications() {
    this.alertMessage = '';
    this.errorMessage = '';

    this.notificationService.getNotifications(this.currentUserId).subscribe({
      next: (res) => {
        this.notifications = res || [];
        this.applyFilters();
        this.refreshUnreadCount();
      },
      error: () => this.errorMessage = 'Failed to load notifications.'
    });
  }

  refreshUnreadCount() {
    this.notificationService.getUnreadCount(this.currentUserId).subscribe({
      next: (count) => this.unreadCount = count
    });
  }

  setStatusFilter(status: string) {
    this.statusFilter = status;
    this.applyFilters();
  }

  setCategoryFilter(category: string) {
    this.categoryFilter = category;
    this.applyFilters();
  }

  private applyFilters() {
    let list = [...this.notifications];

    if (this.statusFilter !== 'ALL') {
      list = list.filter(n => n.status === this.statusFilter);
    } else {
      // By default do not show dismissed ones in "ALL" unless statusFilter explicitly selected.
      list = list.filter(n => n.status !== 'DISMISSED');
    }

    if (this.categoryFilter !== 'ALL') {
      list = list.filter(n => n.category === this.categoryFilter);
    }

    // Sort: newest first
    list.sort((a, b) => new Date(b.createdDate).getTime() - new Date(a.createdDate).getTime());

    this.filteredNotifications = list;
  }

  getCategoryIcon(cat: string): string {
    switch (cat) {
      case 'USAGE': return '📊';
      case 'BILLING': return '💳';
      case 'FAULT': return '🔧';
      case 'PLAN': return '📋';
      case 'COMPLIANCE': return '⚖️';
      default: return '🔔';
    }
  }

  markAsRead(id: number) {
    this.notificationService.markAsRead(id).subscribe({
      next: () => {
        this.loadNotifications();
      }
    });
  }

  dismissNotification(id: number) {
    this.notificationService.dismissNotification(id).subscribe({
      next: () => {
        this.loadNotifications();
      }
    });
  }

  markAllAsRead() {
    this.notificationService.markAllAsRead(this.currentUserId).subscribe({
      next: () => {
        this.loadNotifications();
      }
    });
  }

  onSubmitNotification() {
    if (this.createForm.invalid) return;
    this.notificationService.createNotification(this.createForm.value).subscribe({
      next: () => {
        this.alertMessage = 'Notification created and dispatched successfully!';
        this.createForm.reset({ category: 'USAGE' });
        // If sent to self, reload list
        if (Number(this.createForm.value.userId) === this.currentUserId) {
          this.loadNotifications();
        }
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to send notification.'
    });
  }
}
