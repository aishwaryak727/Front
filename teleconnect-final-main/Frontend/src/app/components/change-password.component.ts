import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="change-password-container">
      <div class="glass-card change-password-card animate-fade-in">
        <div class="header">
          <h2>🔒 Change Password</h2>
          <p>You must change your password to secure your account.</p>
        </div>

        <form [formGroup]="passwordForm" (ngSubmit)="onSubmit()" class="form-body">
          <div *ngIf="errorMessage" class="alert alert-danger">
            ⚠️ {{ errorMessage }}
          </div>
          <div *ngIf="successMessage" class="alert alert-success">
            🎉 {{ successMessage }}
          </div>

          <div class="form-group">
            <label class="form-label">Current Password</label>
            <input type="password" formControlName="currentPassword" class="form-control" placeholder="••••••••" required>
            <span class="error-msg" *ngIf="isFieldInvalid('currentPassword')">Current password is required</span>
          </div>

          <div class="form-group">
            <label class="form-label">New Password</label>
            <input type="password" formControlName="newPassword" class="form-control" placeholder="••••••••" required>
            <span class="error-msg" *ngIf="isFieldInvalid('newPassword')">New password must be at least 6 characters</span>
          </div>

          <div class="form-group">
            <label class="form-label">Confirm New Password</label>
            <input type="password" formControlName="confirmPassword" class="form-control" placeholder="••••••••" required>
            <span class="error-msg" *ngIf="passwordForm.errors?.['mismatch'] && passwordForm.get('confirmPassword')?.touched">Passwords do not match</span>
          </div>

          <div class="action-buttons">
            <button type="submit" [disabled]="passwordForm.invalid || loading" class="btn btn-primary">
              Change Password
            </button>
            <button type="button" (click)="onCancel()" class="btn btn-secondary">
              Cancel
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .change-password-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 80vh;
      padding: 20px;
    }
    .change-password-card {
      width: 100%;
      max-width: 480px;
      padding: 40px;
    }
    .header {
      margin-bottom: 28px;
    }
    .header h2 {
      font-size: 1.6rem;
      margin-bottom: 8px;
    }
    .header p {
      color: var(--text-secondary);
      font-size: 0.95rem;
    }
    .form-body {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    .action-buttons {
      display: flex;
      gap: 12px;
      margin-top: 12px;
    }
    .action-buttons button {
      flex: 1;
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
export class ChangePasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  loading = false;
  errorMessage = '';
  successMessage = '';

  readonly passwordForm: FormGroup = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: this.passwordMatchValidator });

  passwordMatchValidator(g: FormGroup) {
    return g.get('newPassword')?.value === g.get('confirmPassword')?.value
      ? null : { mismatch: true };
  }

  isFieldInvalid(field: string): boolean {
    const control = this.passwordForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  onSubmit() {
    if (this.passwordForm.invalid) return;

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const payload = {
      currentPassword: this.passwordForm.value.currentPassword,
      newPassword: this.passwordForm.value.newPassword
    };

    this.authService.changePassword(payload).subscribe({
      next: (res) => {
        this.loading = false;
        this.successMessage = res.message || 'Password changed successfully!';
        setTimeout(() => {
          this.router.navigate(['/profile']);
        }, 2000);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to change password. Please verify current password.';
      }
    });
  }

  onCancel() {
    this.router.navigate(['/profile']);
  }
}
