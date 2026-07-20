import { Routes } from '@angular/router';
import { LoginComponent } from './components/login.component';
import { RegisterComponent } from './components/register.component';
import { UnauthorizedComponent } from './components/unauthorized.component';
import { LayoutComponent } from './components/layout.component';
import { ProfileComponent } from './components/profile.component';
import { ChangePasswordComponent } from './components/change-password.component';
import { AdminDashboardComponent } from './components/admin-dashboard.component';
import { AuditLogsComponent } from './components/audit-logs.component';
import { SubscriberDashboardComponent } from './components/subscriber-dashboard.component';
import { PlansAddonsComponent } from './components/plans-addons.component';
import { UsageDashboardComponent } from './components/usage-dashboard.component';
import { BillingDashboardComponent } from './components/billing-dashboard.component';
import { FaultDashboardComponent } from './components/fault-dashboard.component';
import { AnalyticsDashboardComponent } from './components/analytics-dashboard.component';
import { NotificationsDashboardComponent } from './components/notifications-dashboard.component';
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
        path: 'plans',
        component: PlansAddonsComponent,
        canActivate: [roleGuard],
        data: { permissions: ['VIEW_PLAN', 'VIEW_OWN_PLAN'] }
      },
      {
        path: 'usage',
        component: UsageDashboardComponent,
        canActivate: [roleGuard],
        data: { permissions: ['USAGE_RECORDS'] }
      },
      {
        path: 'billing',
        component: BillingDashboardComponent,
        canActivate: [roleGuard],
        data: { permissions: ['VIEW_INVOICE', 'PAY_BILL', 'BILLING_CYCLE'] }
      },
      {
        path: 'faults',
        component: FaultDashboardComponent,
        canActivate: [roleGuard],
        data: { permissions: ['SERVICE_REQUEST', 'GET_UPDATE_TICKET', 'RESOLVE_TICKET'] }
      },
      {
        path: 'analytics',
        component: AnalyticsDashboardComponent,
        canActivate: [roleGuard],
        data: { permissions: ['VIEW_REPORT_ARPU', 'VIEW_REPORT_CHURN', 'VIEW_REPORT_NETWORK_UTILISATION', 'VIEW_REPORT_SLA_COMPLIANCE', 'VIEW_REPORT_COLLECTION_EFFICIENCY', 'VIEW_REPORT_SUBSCRIBER_GROWTH', 'GENERATE_REPORT'] }
      },
      {
        path: 'notifications',
        component: NotificationsDashboardComponent,
        canActivate: [roleGuard],
        data: { permissions: ['VIEW_NOTIFICATIONS'] }
      },
      {
        path: 'subscriber/accounts',
        component: SubscriberDashboardComponent,
        canActivate: [roleGuard],
        data: { permissions: ['VIEW_SUBSCRIBER', 'VIEW_OWN_PLAN'] }
      },
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
