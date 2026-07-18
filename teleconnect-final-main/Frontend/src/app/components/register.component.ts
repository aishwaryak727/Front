import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="register-container">
      <div class="glass-card auth-card animate-fade-in">
        <div class="auth-header">
          <div class="logo-icon">⚡</div>
          <h2>Create Account</h2>
          <p>Join teleConnect platform today</p>
        </div>

        <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="auth-form">
          <div *ngIf="errorMessage" class="alert alert-danger">
            ⚠️ {{ errorMessage }}
          </div>
          <div *ngIf="successMessage" class="alert alert-success">
            🎉 {{ successMessage }}
          </div>

          <div class="form-group">
            <label class="form-label">Full Name</label>
            <input type="text" formControlName="name" class="form-control" placeholder="John Doe" required>
            <span class="error-msg" *ngIf="isFieldInvalid('name')">Name is required</span>
          </div>

          <div class="form-group">
            <label class="form-label">Email Address</label>
            <input type="email" formControlName="email" class="form-control" placeholder="john@example.com" required>
            <span class="error-msg" *ngIf="isFieldInvalid('email')">Please enter a valid email address</span>
          </div>

          <div class="form-group">
            <label class="form-label">Phone Number</label>
            <input type="text" formControlName="phone" class="form-control" placeholder="+1234567890">
          </div>

          <div class="form-group">
            <label class="form-label">Password</label>
            <input type="password" formControlName="password" class="form-control" placeholder="••••••••" required>
            <span class="error-msg" *ngIf="isFieldInvalid('password')">Password must be at least 6 characters</span>
          </div>

          <button type="submit" [disabled]="registerForm.invalid || loading" class="btn btn-primary btn-block">
            <span *ngIf="loading" class="spinner"></span>
            {{ loading ? 'Creating Account...' : 'Register' }}
          </button>
        </form>

        <div class="auth-footer">
          <p>Already have an account? <a routerLink="/login">Sign in here</a></p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .register-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      padding: 20px;
    }
    .auth-card {
      width: 100%;
      max-width: 440px;
      padding: 40px;
    }
    .auth-header {
      text-align: center;
      margin-bottom: 32px;
    }
    .auth-header .logo-icon {
      font-size: 3rem;
      margin-bottom: 12px;
    }
    .auth-header h2 {
      font-size: 1.8rem;
      margin-bottom: 6px;
    }
    .auth-header p {
      color: var(--text-secondary);
      font-size: 0.95rem;
    }
    .auth-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .btn-block {
      width: 100%;
      margin-top: 8px;
    }
    .auth-footer {
      text-align: center;
      margin-top: 24px;
      font-size: 0.9rem;
      color: var(--text-secondary);
    }
    .auth-footer a {
      color: var(--accent-primary);
      text-decoration: none;
      font-weight: 600;
    }
    .auth-footer a:hover {
      text-decoration: underline;
    }
    .alert {
      padding: 12px 16px;
      border-radius: var(--radius-md);
      font-size: 0.9rem;
      margin-bottom: 16px;
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
    .spinner {
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top: 2px solid white;
      border-radius: 50%;
      width: 16px;
      height: 16px;
      animation: spin 1s linear infinite;
      display: inline-block;
    }
    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
  `]
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly registerForm: FormGroup = this.fb.group({
    name: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    phone: [''],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  loading = false;
  errorMessage = '';
  successMessage = '';

  isFieldInvalid(field: string): boolean {
    const control = this.registerForm.get(field);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  onSubmit() {
    if (this.registerForm.invalid) return;

    this.loading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.register(this.registerForm.value).subscribe({
      next: (res) => {
        this.loading = false;
        this.successMessage = res.message || 'Registration successful! Redirecting to login...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Registration failed. Try again.';
      }
    });
  }
}
