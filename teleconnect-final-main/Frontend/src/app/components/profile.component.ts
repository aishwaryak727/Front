import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { UserService, UserResponse } from '../services/user.service';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="profile-container animate-fade-in">
      <header class="profile-header">
        <h1>My Account</h1>
        <p>Manage your profile settings and secure your credentials</p>
      </header>

      <div class="profile-grid">
        <!-- Main Profile Info Form -->
        <div class="glass-card main-profile-card">
          <h3>Profile Details</h3>
          <div *ngIf="successMessage" class="alert alert-success mt-12">
            🎉 {{ successMessage }}
          </div>
          <div *ngIf="errorMessage" class="alert alert-danger mt-12">
            ⚠️ {{ errorMessage }}
          </div>

          <form [formGroup]="profileForm" (ngSubmit)="onSubmit()" class="profile-form mt-20">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Full Name</label>
                <input type="text" formControlName="name" class="form-control" [readonly]="!isEditing">
                <span class="error-msg" *ngIf="isFieldInvalid('name')">Name is required</span>
              </div>
              <div class="form-group">
                <label class="form-label">Phone Number</label>
                <input type="text" formControlName="phone" class="form-control" [readonly]="!isEditing">
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Email Address (Read-only)</label>
                <input type="email" [value]="user?.email" class="form-control readonly-input" readonly>
              </div>
              <div class="form-group">
                <label class="form-label">Role</label>
                <div class="role-badge-container">
                  <span class="badge badge-info">{{ user?.roleName }}</span>
                </div>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Status</label>
                <div>
                  <span class="badge" [ngClass]="user?.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'">
                    {{ user?.status }}
                  </span>
                </div>
              </div>
              <div class="form-group">
                <label class="form-label">Created At</label>
                <input type="text" [value]="user?.createdAt | date:'medium'" class="form-control readonly-input" readonly>
              </div>
            </div>

            <div class="form-actions mt-20">
              <button *ngIf="!isEditing" type="button" (click)="toggleEdit()" class="btn btn-secondary">
                Edit Profile
              </button>
              <button *ngIf="isEditing" type="submit" [disabled]="profileForm.invalid || loading" class="btn btn-primary">
                {{ loading ? 'Saving...' : 'Save Changes' }}
              </button>
              <button *ngIf="isEditing" type="button" (click)="cancelEdit()" class="btn btn-secondary">
                Cancel
              </button>
            </div>
          </form>
        </div>

        <!-- Security and Permissions Cards -->
        <div class="side-panels">
          <div class="glass-card security-card">
            <h3>Security</h3>
            <p class="description">Keep your password updated to ensure account safety.</p>
            <button routerLink="/change-password" class="btn btn-secondary btn-block mt-16">
              🔑 Change Password
            </button>
          </div>

          <div class="glass-card permissions-card mt-20">
            <h3>Permissions Granted</h3>
            <p class="description">Actions you are allowed to perform in teleConnect:</p>
            <ul class="permission-list mt-12">
              <li *ngFor="let perm of authService.userPermissions()">
                <span class="check-icon">✓</span> {{ perm }}
              </li>
              <li *ngIf="authService.userPermissions().length === 0" class="muted-text">
                No custom permissions assigned.
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .profile-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .profile-header h1 {
      font-size: 2.2rem;
      margin-bottom: 6px;
    }
    .profile-header p {
      color: var(--text-secondary);
      font-size: 1rem;
    }
    .profile-grid {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: 30px;
    }
    .main-profile-card h3, .security-card h3, .permissions-card h3 {
      font-size: 1.3rem;
      margin-bottom: 8px;
    }
    .profile-form {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    .form-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
    }
    .readonly-input {
      background: rgba(255, 255, 255, 0.01) !important;
      border-color: rgba(255, 255, 255, 0.03);
      color: var(--text-secondary);
      cursor: not-allowed;
    }
    .role-badge-container {
      padding: 10px 0;
    }
    .form-actions {
      display: flex;
      gap: 12px;
    }
    .side-panels {
      display: flex;
      flex-direction: column;
    }
    .description {
      font-size: 0.9rem;
      color: var(--text-secondary);
    }
    .permission-list {
      list-style: none;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .permission-list li {
      font-size: 0.85rem;
      font-weight: 500;
      color: #ffffff;
      background: rgba(255, 255, 255, 0.02);
      padding: 6px 12px;
      border-radius: var(--radius-sm);
      border: 1px solid var(--border-color);
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .check-icon {
      color: var(--success);
      font-weight: bold;
    }
    .muted-text {
      color: var(--text-muted);
      font-style: italic;
    }
    .mt-12 { margin-top: 12px; }
    .mt-16 { margin-top: 16px; }
    .mt-20 { margin-top: 20px; }
    .btn-block { width: 100%; }
    
    @media (max-width: 992px) {
      .profile-grid {
        grid-template-columns: 1fr;
      }
    }
    @media (max-width: 576px) {
      .form-row {
        grid-template-columns: 1fr;
      }
    }
    .alert {
      padding: 12px 16px;
      border-radius: var(--radius-md);
      font-size: 0.9rem;
    }
    .alert-danger {
      background: var(--danger-glow);
      color: var(--danger);
      border: 1px solid rgba(239, 68, 68, 0.2);
    }
    .alert-success {
      background: var(--success-glow);
      color: var(--success);
      border: 1px solid rgba(16, 185, 129, 0.2);
    }
    .error-msg {
      font-size: 0.8rem;
      color: var(--danger);
    }
  `]
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  readonly authService = inject(AuthService);

  user: UserResponse | null = null;
  profileForm!: FormGroup;
  isEditing = false;
  loading = false;
  successMessage = '';
  errorMessage = '';

  ngOnInit() {
    this.loadProfile();
  }

  loadProfile() {
    this.userService.getMe().subscribe({
      next: (res) => {
        this.user = res;
        this.initForm();
      },
      error: (err) => {
        this.errorMessage = 'Failed to load profile details.';
      }
    });
  }

  initForm() {
    this.profileForm = this.fb.group({
      name: [this.user?.name || '', [Validators.required]],
      phone: [this.user?.phone || '']
    });
  }

  toggleEdit() {
    this.isEditing = true;
    this.successMessage = '';
    this.errorMessage = '';
  }

  cancelEdit() {
    this.isEditing = false;
    this.initForm();
  }

  isFieldInvalid(field: string): boolean {
    const control = this.profileForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  onSubmit() {
    if (this.profileForm.invalid || !this.user) return;

    this.loading = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.userService.updateUser(this.user.userId, this.profileForm.value).subscribe({
      next: (res) => {
        this.loading = false;
        this.isEditing = false;
        this.successMessage = res.message || 'Profile updated successfully!';
        this.loadProfile();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to update profile.';
      }
    });
  }
}
