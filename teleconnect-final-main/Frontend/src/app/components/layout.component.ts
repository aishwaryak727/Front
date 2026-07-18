import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet, RouterLinkActive } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="layout-container">
      <!-- Sidebar -->
      <aside class="sidebar">
        <div class="logo-area">
          <span class="logo-icon">⚡</span>
          <span class="logo-text">teleConnect</span>
        </div>
        
        <div class="user-profile-summary">
          <div class="avatar">{{ getInitials() }}</div>
          <div class="info">
            <div class="username">{{ authService.currentUser()?.name }}</div>
            <div class="role-badge">{{ authService.userRole() }}</div>
          </div>
        </div>

        <nav class="nav-menu">
          <a routerLink="/profile" routerLinkActive="active" class="nav-item">
            <span class="icon">👤</span> Profile
          </a>

          <!-- Admin & CS Agent Only -->
          <a *ngIf="authService.hasAnyPermission(['VIEW_ALL_USERS', 'VIEW_SUBSCRIBER'])" 
             routerLink="/admin/dashboard" routerLinkActive="active" class="nav-item">
            <span class="icon">⚙️</span> User Manager
          </a>

          <!-- Admin & Compliance Only -->
          <a *ngIf="authService.hasPermission('VIEW_AUDIT_LOGS')" 
             routerLink="/admin/audit-logs" routerLinkActive="active" class="nav-item">
            <span class="icon">📋</span> Audit Logs
          </a>
        </nav>

        <div class="sidebar-footer">
          <button (click)="logout()" class="btn btn-secondary btn-logout">
            <span>🚪</span> Log Out
          </button>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="main-content">
        <div class="page-wrapper animate-fade-in">
          <router-outlet></router-outlet>
        </div>
      </main>
    </div>
  `,
  styles: [`
    .layout-container {
      display: flex;
      min-height: 100vh;
    }
    .sidebar {
      width: 260px;
      background: var(--bg-secondary);
      border-right: 1px solid var(--border-color);
      display: flex;
      flex-direction: column;
      padding: 24px;
      position: fixed;
      height: 100vh;
      z-index: 10;
    }
    .logo-area {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 32px;
    }
    .logo-icon {
      font-size: 1.8rem;
    }
    .logo-text {
      font-family: var(--font-display);
      font-size: 1.4rem;
      font-weight: 800;
      background: linear-gradient(135deg, #ffffff, var(--accent-primary));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .user-profile-summary {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: var(--bg-tertiary);
      border-radius: var(--radius-md);
      margin-bottom: 32px;
      border: 1px solid var(--border-color);
    }
    .avatar {
      width: 42px;
      height: 42px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      color: white;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 1rem;
    }
    .info {
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    .username {
      font-size: 0.9rem;
      font-weight: 600;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
    }
    .role-badge {
      font-size: 0.7rem;
      color: var(--text-secondary);
      text-transform: uppercase;
      font-weight: 700;
      letter-spacing: 0.05em;
    }
    .nav-menu {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex-grow: 1;
    }
    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px 16px;
      color: var(--text-secondary);
      text-decoration: none;
      border-radius: var(--radius-md);
      font-weight: 500;
      transition: var(--transition-smooth);
    }
    .nav-item:hover {
      background: rgba(255, 255, 255, 0.03);
      color: #ffffff;
    }
    .nav-item.active {
      background: var(--accent-glow);
      color: var(--accent-primary);
      font-weight: 600;
      border-left: 3px solid var(--accent-primary);
      border-radius: 0 var(--radius-md) var(--radius-md) 0;
    }
    .sidebar-footer {
      padding-top: 16px;
      border-top: 1px solid var(--border-color);
    }
    .btn-logout {
      width: 100%;
      justify-content: center;
      background: rgba(239, 68, 68, 0.08);
      color: #ef4444;
      border: 1px solid rgba(239, 68, 68, 0.2);
    }
    .btn-logout:hover {
      background: #ef4444;
      color: white;
    }
    .main-content {
      flex-grow: 1;
      margin-left: 260px;
      padding: 40px;
      background: var(--bg-primary);
      min-height: 100vh;
    }
    .page-wrapper {
      max-width: 1200px;
      margin: 0 auto;
    }
    @media (max-width: 768px) {
      .sidebar {
        width: 80px;
        padding: 16px 8px;
        align-items: center;
      }
      .logo-text, .username, .role-badge, .nav-item span:not(.icon) {
        display: none;
      }
      .user-profile-summary {
        padding: 4px;
        background: transparent;
        border: none;
      }
      .main-content {
        margin-left: 80px;
        padding: 20px;
      }
    }
  `]
})
export class LayoutComponent {
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  getInitials(): string {
    const name = this.authService.currentUser()?.name || '';
    return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  }

  logout() {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'])
    });
  }
}
