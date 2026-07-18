import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { AdminService, AuditLogResponse, PageResponse } from '../services/admin.service';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="audit-container animate-fade-in">
      <header class="audit-header">
        <h1>System Audit Logs</h1>
        <p>Compliance and security logs tracking system access and administrative actions</p>
      </header>

      <!-- Filter Panel -->
      <div class="glass-card mb-24">
        <h3>Search & Filter Logs</h3>
        <form [formGroup]="filterForm" (ngSubmit)="onSearch()" class="filter-grid mt-16">
          <div class="form-group">
            <label class="form-label">User ID (Optional)</label>
            <input type="number" formControlName="userId" class="form-control" placeholder="Filter by User ID">
          </div>
          <div class="form-group">
            <label class="form-label">Action</label>
            <input type="text" formControlName="action" class="form-control" placeholder="e.g. USER_LOGIN">
          </div>
          <div class="form-group">
            <label class="form-label">Module</label>
            <select formControlName="module" class="form-control form-select">
              <option value="">All Modules</option>
              <option value="IAM">IAM (Identity)</option>
              <option value="SUBSCRIBER">Subscriber</option>
              <option value="BILLING">Billing</option>
              <option value="FAULT">Fault Manager</option>
              <option value="PLAN">Plan Manager</option>
              <option value="ANALYTICS">Analytics</option>
            </select>
          </div>
          <div class="form-group btn-align">
            <button type="submit" class="btn btn-primary btn-block">Filter</button>
            <button type="button" (click)="resetFilters()" class="btn btn-secondary btn-block">Reset</button>
          </div>
        </form>
      </div>

      <!-- Logs Card -->
      <div class="glass-card">
        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Audit ID</th>
                <th>User ID</th>
                <th>Action performed</th>
                <th>Module</th>
                <th>IP Address</th>
                <th>Timestamp</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let log of logs">
                <td><code>#{{ log.auditId }}</code></td>
                <td>
                  <span class="user-pill" (click)="filterByUser(log.userId)">
                    👤 User {{ log.userId }}
                  </span>
                </td>
                <td><strong>{{ log.action }}</strong></td>
                <td><span class="badge badge-info">{{ log.module }}</span></td>
                <td><code>{{ log.ipAddress }}</code></td>
                <td>{{ log.timestamp | date:'medium' }}</td>
              </tr>
              <tr *ngIf="logs.length === 0">
                <td colspan="6" class="text-center muted-text py-20">No audit logs found.</td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination Controls -->
        <div class="pagination mt-20" *ngIf="totalPages > 1">
          <button [disabled]="currentPage === 0" (click)="goToPage(currentPage - 1)" class="btn btn-secondary btn-small">
            ← Previous
          </button>
          <span class="pagination-info">
            Page {{ currentPage + 1 }} of {{ totalPages }} ({{ totalElements }} logs total)
          </span>
          <button [disabled]="currentPage === totalPages - 1" (click)="goToPage(currentPage + 1)" class="btn btn-secondary btn-small">
            Next →
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .audit-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .audit-header h1 {
      font-size: 2.2rem;
      margin-bottom: 6px;
    }
    .audit-header p {
      color: var(--text-secondary);
      font-size: 1rem;
    }
    .filter-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      align-items: end;
    }
    .btn-align {
      display: flex;
      gap: 10px;
    }
    .user-pill {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--border-color);
      padding: 4px 8px;
      border-radius: var(--radius-sm);
      font-size: 0.85rem;
      cursor: pointer;
      font-weight: 500;
      transition: var(--transition-smooth);
    }
    .user-pill:hover {
      background: var(--accent-glow);
      border-color: var(--accent-primary);
      color: var(--accent-primary);
    }
    .pagination {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 16px;
      border-top: 1px solid var(--border-color);
    }
    .pagination-info {
      font-size: 0.9rem;
      color: var(--text-secondary);
    }
    .mb-24 { margin-bottom: 24px; }
    .mt-16 { margin-top: 16px; }
    .mt-20 { margin-top: 20px; }
    .py-20 { padding-top: 20px; padding-bottom: 20px; }
    .btn-block { width: 100%; }
  `]
})
export class AuditLogsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly fb = inject(FormBuilder);

  logs: AuditLogResponse[] = [];
  filterForm!: FormGroup;

  // Pagination states
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  ngOnInit() {
    this.initFilterForm();
    this.loadLogs();
  }

  initFilterForm() {
    this.filterForm = this.fb.group({
      userId: [''],
      action: [''],
      module: ['']
    });
  }

  loadLogs() {
    const filters = this.filterForm.value;
    const requestFilters = {
      action: filters.action || undefined,
      module: filters.module || undefined,
      page: this.currentPage,
      size: this.pageSize
    };

    const handleResponse = (res: PageResponse<AuditLogResponse>) => {
      this.logs = res.content;
      this.totalPages = res.totalPages;
      this.totalElements = res.totalElements;
    };

    if (filters.userId) {
      this.adminService.getLogsByUser(filters.userId, requestFilters).subscribe({
        next: handleResponse
      });
    } else {
      this.adminService.getAuditLogs(requestFilters).subscribe({
        next: handleResponse
      });
    }
  }

  onSearch() {
    this.currentPage = 0;
    this.loadLogs();
  }

  filterByUser(userId: number) {
    this.filterForm.patchValue({ userId });
    this.currentPage = 0;
    this.loadLogs();
  }

  resetFilters() {
    this.filterForm.reset({
      userId: '', action: '', module: ''
    });
    this.currentPage = 0;
    this.loadLogs();
  }

  goToPage(page: number) {
    this.currentPage = page;
    this.loadLogs();
  }
}
