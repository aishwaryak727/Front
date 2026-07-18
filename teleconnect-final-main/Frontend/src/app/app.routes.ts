import { Routes } from '@angular/router';
import { LoginComponent } from './components/login.component';
import { RegisterComponent } from './components/register.component';
import { UnauthorizedComponent } from './components/unauthorized.component';
import { LayoutComponent } from './components/layout.component';
import { ProfileComponent } from './components/profile.component';
import { ChangePasswordComponent } from './components/change-password.component';
import { AdminDashboardComponent } from './components/admin-dashboard.component';
import { AuditLogsComponent } from './components/audit-logs.component';
import { authGuard } from './guards/auth.guard';
import { roleGuard } from './guards/role.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'unauthorized', component: UnauthorizedComponent },
  {
    path: '',
    component: LayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'profile', pathMatch: 'full' },
      { path: 'profile', component: ProfileComponent },
      { path: 'change-password', component: ChangePasswordComponent },
      {
        path: 'admin/dashboard',
        component: AdminDashboardComponent,
        canActivate: [roleGuard],
        data: { permissions: ['VIEW_ALL_USERS', 'VIEW_SUBSCRIBER'] }
      },
      {
        path: 'admin/audit-logs',
        component: AuditLogsComponent,
        canActivate: [roleGuard],
        data: { permissions: ['VIEW_AUDIT_LOGS'] }
      }
    ]
  },
  { path: '**', redirectTo: 'profile' }
];
