import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { UserService, UserResponse } from '../services/user.service';
import { AdminService, Role } from '../services/admin.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="admin-container animate-fade-in">
      <header class="admin-header">
        <h1>User Manager</h1>
        <p>Control users, create staff accounts, manage roles and verify compliance status</p>
      </header>

      <!-- Tabs Navigation -->
      <div class="tabs">
        <button [class.active]="activeTab === 'users'" (click)="setTab('users')" class="tab-btn">
          👥 User Directory
        </button>
        <button *ngIf="authService.hasPermission('CREATE_USER')"
                [class.active]="activeTab === 'createStaff'" (click)="setTab('createStaff')" class="tab-btn">
          ➕ Create Staff Account
        </button>
        <button [class.active]="activeTab === 'roles'" (click)="setTab('roles')" class="tab-btn">
          🔑 Roles & Permissions
        </button>
      </div>

      <!-- User Directory Tab -->
      <div *ngIf="activeTab === 'users'" class="tab-content animate-fade-in">
        <!-- Search Filters Card -->
        <div class="glass-card filters-card mb-24">
          <h3>Filter Search</h3>
          <form [formGroup]="searchForm" (ngSubmit)="onSearch()" class="search-grid mt-16">
            <div class="form-group">
              <label class="form-label">Name</label>
              <input type="text" formControlName="name" class="form-control" placeholder="Search by name">
            </div>
            <div class="form-group">
              <label class="form-label">Email</label>
              <input type="text" formControlName="email" class="form-control" placeholder="Search by email">
            </div>
            <div class="form-group">
              <label class="form-label">Phone</label>
              <input type="text" formControlName="phone" class="form-control" placeholder="Search by phone">
            </div>
            <div class="form-group">
              <label class="form-label">Role</label>
              <select formControlName="role" class="form-control form-select">
                <option value="">All Roles</option>
                <option *ngFor="let role of roles" [value]="role.roleName">{{ role.roleName }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Status</label>
              <select formControlName="status" class="form-control form-select">
                <option value="">All Statuses</option>
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
                <option value="PENDING">PENDING</option>
              </select>
            </div>
            <div class="form-group btn-align">
              <button type="submit" class="btn btn-primary btn-block">Search</button>
              <button type="button" (click)="resetSearch()" class="btn btn-secondary btn-block">Clear</button>
            </div>
          </form>
        </div>

        <div *ngIf="alertMessage" class="alert alert-info mb-16">
          ℹ️ {{ alertMessage }}
        </div>

        <!-- Users Table -->
        <div class="glass-card">
          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let u of users">
                  <td>{{ u.userId }}</td>
                  <td>{{ u.name }}</td>
                  <td>{{ u.email }}</td>
                  <td>{{ u.phone || 'N/A' }}</td>
                  <td><span class="badge badge-info">{{ u.roleName }}</span></td>
                  <td>
                    <span class="badge" [ngClass]="u.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'">
                      {{ u.status }}
                    </span>
                  </td>
                  <td class="action-cell">
                    <!-- Toggle Status (Admin Only - requires DELETE_USER) -->
                    <button *ngIf="authService.hasPermission('DELETE_USER')"
                            (click)="toggleStatus(u)"
                            class="btn btn-secondary btn-small"
                            [title]="u.status === 'ACTIVE' ? 'Deactivate user' : 'Activate user'">
                      {{ u.status === 'ACTIVE' ? 'Deactivate' : 'Activate' }}
                    </button>
                    <!-- Reset Password (Admin Only - requires CREATE_USER) -->
                    <button *ngIf="authService.hasPermission('CREATE_USER')"
                            (click)="resetPassword(u.userId)"
                            class="btn btn-secondary btn-small">
                      🔄 Reset Pass
                    </button>
                  </td>
                </tr>
                <tr *ngIf="users.length === 0">
                  <td colspan="7" class="text-center muted-text py-20">No users found matching the search criteria.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Create Staff Tab -->
      <div *ngIf="activeTab === 'createStaff'" class="tab-content animate-fade-in">
        <div class="glass-card max-width-600">
          <h3>Create Staff Account</h3>
          <p class="description mb-20">Create internal staff members like Customer Service Agents or Network Operators.</p>
          
          <div *ngIf="createSuccess" class="alert alert-success mb-16">
            🎉 {{ createSuccess }}
          </div>
          <div *ngIf="createError" class="alert alert-danger mb-16">
            ⚠️ {{ createError }}
          </div>

          <form [formGroup]="staffForm" (ngSubmit)="onCreateStaff()" class="staff-form">
            <div class="form-group">
              <label class="form-label">Full Name</label>
              <input type="text" formControlName="name" class="form-control" placeholder="Jane Doe" required>
              <span class="error-msg" *ngIf="isStaffFieldInvalid('name')">Name is required</span>
            </div>

            <div class="form-group">
              <label class="form-label">Email Address</label>
              <input type="email" formControlName="email" class="form-control" placeholder="jane@teleconnect.com" required>
              <span class="error-msg" *ngIf="isStaffFieldInvalid('email')">Please enter a valid email</span>
            </div>

            <div class="form-group">
              <label class="form-label">Phone Number</label>
              <input type="text" formControlName="phone" class="form-control" placeholder="+1234567890">
            </div>

            <div class="form-group">
              <label class="form-label">Role</label>
              <select formControlName="roleName" class="form-control form-select" required>
                <option value="" disabled>Select Staff Role</option>
                <option *ngFor="let role of staffRolesOnly()" [value]="role.roleName">{{ role.roleName }}</option>
              </select>
              <span class="error-msg" *ngIf="isStaffFieldInvalid('roleName')">Role is required</span>
            </div>

            <button type="submit" [disabled]="staffForm.invalid || creating" class="btn btn-primary mt-12">
              {{ creating ? 'Creating...' : 'Create Account' }}
            </button>
          </form>
        </div>
      </div>

      <!-- Roles & Permissions Tab -->
      <div *ngIf="activeTab === 'roles'" class="tab-content animate-fade-in">
        <div class="roles-grid">
          <!-- Roles list panel -->
          <div class="glass-card">
            <h3>Roles</h3>
            <p class="description mb-16">Click a role to view assigned permissions</p>
            <div class="roles-list">
              <div *ngFor="let role of roles"
                   [class.active]="selectedRole?.roleId === role.roleId"
                   (click)="selectRole(role)"
                   class="role-item">
                <div class="role-name">{{ role.roleName }}</div>
                <div class="role-arrow">→</div>
              </div>
            </div>
          </div>

          <!-- Permissions list panel -->
          <div class="glass-card">
            <h3>Permissions for {{ selectedRole?.roleName || 'Select a Role' }}</h3>
            <div *ngIf="selectedRole" class="permissions-details-list mt-16 animate-fade-in">
              <div *ngFor="let perm of selectedRole.permissions" class="permission-detail-item">
                <div class="perm-title">{{ perm.permissionName }}</div>
                <div class="perm-desc">{{ perm.description || 'No description provided.' }}</div>
              </div>
              <div *ngIf="selectedRole.permissions.length === 0" class="muted-text text-center py-20">
                No permissions assigned to this role.
              </div>
            </div>
            <div *ngIf="!selectedRole" class="muted-text text-center py-40">
              Please choose a role from the left column to inspect its permissions.
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .admin-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .admin-header h1 {
      font-size: 2.2rem;
      margin-bottom: 6px;
    }
    .admin-header p {
      color: var(--text-secondary);
      font-size: 1rem;
    }
    .tabs {
      display: flex;
      gap: 12px;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 12px;
    }
    .tab-btn {
      background: transparent;
      border: none;
      color: var(--text-secondary);
      font-size: 1rem;
      font-weight: 600;
      padding: 8px 16px;
      cursor: pointer;
      border-radius: var(--radius-md);
      transition: var(--transition-smooth);
    }
    .tab-btn:hover {
      color: white;
      background: rgba(255, 255, 255, 0.02);
    }
    .tab-btn.active {
      color: var(--accent-primary);
      background: var(--accent-glow);
    }
    .search-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      align-items: end;
    }
    .btn-align {
      display: flex;
      gap: 10px;
    }
    .action-cell {
      display: flex;
      gap: 8px;
    }
    .max-width-600 {
      max-width: 600px;
    }
    .description {
      font-size: 0.9rem;
      color: var(--text-secondary);
    }
    .staff-form {
      display: flex;
      flex-direction: column;
      gap: 18px;
    }
    .roles-grid {
      display: grid;
      grid-template-columns: 1fr 2fr;
      gap: 30px;
    }
    .roles-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
    }
    .role-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      cursor: pointer;
      font-weight: 600;
      transition: var(--transition-smooth);
    }
    .role-item:hover {
      background: rgba(255, 255, 255, 0.04);
      border-color: var(--accent-primary);
    }
    .role-item.active {
      background: var(--accent-glow);
      border-color: var(--accent-primary);
      color: var(--accent-primary);
    }
    .role-arrow {
      opacity: 0;
      transition: var(--transition-smooth);
    }
    .role-item:hover .role-arrow, .role-item.active .role-arrow {
      opacity: 1;
      transform: translateX(4px);
    }
    .permissions-details-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .permission-detail-item {
      padding: 14px;
      background: rgba(255, 255, 255, 0.01);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-sm);
    }
    .perm-title {
      font-weight: 700;
      font-size: 0.95rem;
      color: #ffffff;
      margin-bottom: 4px;
    }
    .perm-desc {
      font-size: 0.85rem;
      color: var(--text-secondary);
    }
    .alert {
      padding: 12px 16px;
      border-radius: var(--radius-md);
      font-size: 0.9rem;
    }
    .alert-info {
      background: var(--accent-glow);
      color: var(--accent-primary);
      border: 1px solid rgba(59, 130, 246, 0.2);
    }
    .alert-success {
      background: var(--success-glow);
      color: var(--success);
      border: 1px solid rgba(16, 185, 129, 0.2);
    }
    .alert-danger {
      background: var(--danger-glow);
      color: var(--danger);
      border: 1px solid rgba(239, 68, 68, 0.2);
    }
    .error-msg {
      font-size: 0.8rem;
      color: var(--danger);
    }
    .text-center { text-align: center; }
    .muted-text { color: var(--text-muted); }
    .mb-16 { margin-bottom: 16px; }
    .mb-24 { margin-bottom: 24px; }
    .mt-16 { margin-top: 16px; }
    .mt-12 { margin-top: 12px; }
    .py-20 { padding-top: 20px; padding-bottom: 20px; }
    .py-40 { padding-top: 40px; padding-bottom: 40px; }
    .btn-block { width: 100%; }
  `]
})
export class AdminDashboardComponent implements OnInit {
  private readonly userService = inject(UserService);
  private readonly adminService = inject(AdminService);
  readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  activeTab: 'users' | 'createStaff' | 'roles' = 'users';
  users: UserResponse[] = [];
  roles: Role[] = [];
  selectedRole: Role | null = null;

  searchForm!: FormGroup;
  staffForm!: FormGroup;

  // Feedback states
  alertMessage = '';
  createSuccess = '';
  createError = '';
  creating = false;

  ngOnInit() {
    this.initSearchForm();
    this.initStaffForm();
    this.loadRoles();
    this.loadUsers();
  }

  setTab(tab: 'users' | 'createStaff' | 'roles') {
    this.activeTab = tab;
    this.alertMessage = '';
    this.createSuccess = '';
    this.createError = '';
  }

  initSearchForm() {
    this.searchForm = this.fb.group({
      name: [''],
      email: [''],
      phone: [''],
      role: [''],
      status: ['']
    });
  }

  initStaffForm() {
    this.staffForm = this.fb.group({
      name: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      roleName: ['', [Validators.required]]
    });
  }

  loadUsers() {
    this.userService.getAllUsers().subscribe({
      next: (res) => this.users = res,
      error: () => this.alertMessage = 'Failed to load user directory.'
    });
  }

  loadRoles() {
    this.adminService.getRoles().subscribe({
      next: (res) => {
        this.roles = res;
        if (res.length > 0) this.selectRole(res[0]);
      }
    });
  }

  selectRole(role: Role) {
    this.adminService.getRolePermissions(role.roleId).subscribe({
      next: (res) => this.selectedRole = res
    });
  }

  staffRolesOnly(): Role[] {
    return this.roles.filter(r => r.roleName !== 'SUBSCRIBER');
  }

  onSearch() {
    this.userService.searchUsers(this.searchForm.value).subscribe({
      next: (res) => this.users = res,
      error: () => this.alertMessage = 'Failed to search users.'
    });
  }

  resetSearch() {
    this.searchForm.reset({
      name: '', email: '', phone: '', role: '', status: ''
    });
    this.loadUsers();
  }

  toggleStatus(user: UserResponse) {
    const newStatus = user.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.adminService.updateStatus(user.userId, newStatus).subscribe({
      next: (res) => {
        this.alertMessage = res.message || `User status updated to ${newStatus}.`;
        this.loadUsers();
      },
      error: (err) => this.alertMessage = err.error?.message || 'Failed to update user status.'
    });
  }

  resetPassword(userId: number) {
    if (!confirm('Are you sure you want to reset this user\'s password to default?')) return;
    
    this.adminService.resetPassword(userId).subscribe({
      next: (res) => this.alertMessage = res.message || 'Password reset successfully.',
      error: (err) => this.alertMessage = err.error?.message || 'Failed to reset password.'
    });
  }

  isStaffFieldInvalid(field: string): boolean {
    const control = this.staffForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  onCreateStaff() {
    if (this.staffForm.invalid) return;

    this.creating = true;
    this.createSuccess = '';
    this.createError = '';

    this.adminService.createStaff(this.staffForm.value).subscribe({
      next: (res) => {
        this.creating = false;
        this.createSuccess = res.message || 'Staff account created successfully! Check email for password.';
        this.staffForm.reset({ roleName: '' });
        this.loadUsers();
      },
      error: (err) => {
        this.creating = false;
        this.createError = err.error?.message || 'Failed to create staff account.';
      }
    });
  }
}
