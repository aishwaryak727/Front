import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-unauthorized',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="unauthorized-container animate-fade-in">
      <div class="glass-card error-card">
        <div class="icon">🚫</div>
        <h2>Access Denied</h2>
        <p class="description mt-12">
          You do not have the required permissions to access this page. Please contact your system administrator if you believe this is an error.
        </p>
        <div class="actions mt-24">
          <button routerLink="/profile" class="btn btn-primary">
            Back to Profile
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .unauthorized-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 70vh;
      padding: 20px;
    }
    .error-card {
      max-width: 480px;
      text-align: center;
      padding: 40px;
    }
    .icon {
      font-size: 4rem;
      margin-bottom: 16px;
    }
    .description {
      color: var(--text-secondary);
      font-size: 0.95rem;
      line-height: 1.6;
    }
    .mt-12 { margin-top: 12px; }
    .mt-24 { margin-top: 24px; }
  `]
})
export class UnauthorizedComponent {}
