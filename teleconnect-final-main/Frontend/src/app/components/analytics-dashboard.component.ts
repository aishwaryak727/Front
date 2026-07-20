import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { AnalyticsService, DashboardResponse, TelecomReportResponse, Page, ReportGenerationRequest, RegionalAnalysis, SegmentAnalysis } from '../services/analytics.service';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="analytics-container animate-fade-in">
      <header class="analytics-header">
        <h1>Executive Analytics & Reports</h1>
        <p>Monitor high-level KPI trends, generate snapshot reports, and download data exports</p>
      </header>

      <!-- Main Tabs Navigation -->
      <div class="tabs">
        <button [class.active]="activeTab === 'dashboard'" (click)="setTab('dashboard')" class="tab-btn">
          📊 Executive Dashboard
        </button>
        <button [class.active]="activeTab === 'snapshots'" (click)="setTab('snapshots')" class="tab-btn">
          📋 Saved Snapshots
        </button>
        <button [class.active]="activeTab === 'adhoc'" (click)="setTab('adhoc')" class="tab-btn">
          ⚡ Ad-Hoc Calculators
        </button>
      </div>

      <!-- Alerts -->
      <div *ngIf="alertMessage" class="alert alert-info mt-16">
        ℹ️ {{ alertMessage }}
      </div>
      <div *ngIf="successMessage" class="alert alert-success mt-16">
        🎉 {{ successMessage }}
      </div>
      <div *ngIf="errorMessage" class="alert alert-danger mt-16">
        ⚠️ {{ errorMessage }}
      </div>

      <!-- Executive Dashboard Tab -->
      <div *ngIf="activeTab === 'dashboard'" class="tab-content animate-fade-in">
        <!-- Date / Cycle Filters Card -->
        <div class="glass-card mb-24">
          <div class="filter-layout">
            <div class="form-group mb-0">
              <label class="form-label">Billing Cycle ID</label>
              <input type="number" [(ngModel)]="cycleId" class="form-control">
            </div>
            <div class="form-group mb-0">
              <label class="form-label">Start Date</label>
              <input type="date" [(ngModel)]="startDate" class="form-control">
            </div>
            <div class="form-group mb-0">
              <label class="form-label">End Date</label>
              <input type="date" [(ngModel)]="endDate" class="form-control">
            </div>
            <div class="filter-actions">
              <button (click)="loadDashboard()" class="btn btn-primary">Apply Filters</button>
              <button (click)="downloadDashboardPdf()" class="btn btn-secondary">
                📥 Export Dashboard PDF
              </button>
            </div>
          </div>
        </div>

        <div *ngIf="dashboardData" class="animate-fade-in">
          <!-- KPI Metrics Grid -->
          <div class="kpi-grid mb-24">
            <div class="glass-card kpi-card">
              <h4>ACTIVE SUBSCRIBERS</h4>
              <div class="huge-number mt-8 text-primary">{{ dashboardData.kpis.activeSubscribers | number }}</div>
            </div>
            <div class="glass-card kpi-card">
              <h4>AVERAGE REVENUE (ARPU)</h4>
              <div class="huge-number mt-8 text-success">\${{ dashboardData.kpis.arpu | number:'1.2-2' }}</div>
            </div>
            <div class="glass-card kpi-card">
              <h4>SLA COMPLIANCE</h4>
              <div class="huge-number mt-8 text-success">{{ dashboardData.kpis.slaCompliance | number:'1.0-2' }}%</div>
            </div>
            <div class="glass-card kpi-card">
              <h4>COLLECTION EFFICIENCY</h4>
              <div class="huge-number mt-8 text-success">{{ dashboardData.kpis.collectionEfficiency | number:'1.0-2' }}%</div>
            </div>
            <div class="glass-card kpi-card">
              <h4>CHURN RATE</h4>
              <div class="huge-number mt-8" [ngClass]="dashboardData.kpis.churnRate > 5 ? 'text-danger' : 'text-primary'">
                {{ dashboardData.kpis.churnRate | number:'1.0-2' }}%
              </div>
            </div>
            <div class="glass-card kpi-card">
              <h4>FAULT TICKET COUNT</h4>
              <div class="huge-number mt-8 text-danger">{{ dashboardData.kpis.faultCount }}</div>
            </div>
            <div class="glass-card kpi-card">
              <h4>DISPUTE RATE</h4>
              <div class="huge-number mt-8">{{ dashboardData.kpis.disputeRate | number:'1.0-2' }}%</div>
            </div>
            <div class="glass-card kpi-card">
              <h4>DATA CONSUMED</h4>
              <div class="huge-number mt-8 text-primary">{{ dashboardData.kpis.dataConsumption | number }} MB</div>
            </div>
          </div>

          <!-- Regional & Segment Analysis tables -->
          <div class="analysis-grid mb-24">
            <!-- Regional table -->
            <div class="glass-card">
              <h3>Regional Breakdown Analysis</h3>
              <div class="table-container mt-16">
                <table class="table">
                  <thead>
                    <tr>
                      <th>Region</th>
                      <th>Subscribers</th>
                      <th>Avg ARPU</th>
                      <th>Churn Rate</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let region of getRegionsList(dashboardData.regions)">
                      <td><strong>{{ region.name }}</strong></td>
                      <td>{{ region.subscribers | number }}</td>
                      <td>\${{ region.arpu | number:'1.2-2' }}</td>
                      <td>
                        <span class="badge" [ngClass]="region.churn > 5 ? 'badge-danger' : 'badge-success'">
                          {{ region.churn | number:'1.0-2' }}%
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            <!-- Segment table -->
            <div class="glass-card">
              <h3>Subscriber Segment Analysis</h3>
              <div class="table-container mt-16">
                <table class="table">
                  <thead>
                    <tr>
                      <th>Account Segment</th>
                      <th>Subscribers</th>
                      <th>Avg ARPU</th>
                      <th>Churn Rate</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let seg of getSegmentsList(dashboardData.segments)">
                      <td><strong>{{ seg.name }}</strong></td>
                      <td>{{ seg.subscribers | number }}</td>
                      <td>\${{ seg.arpu | number:'1.2-2' }}</td>
                      <td>
                        <span class="badge" [ngClass]="seg.churn > 5 ? 'badge-danger' : 'badge-success'">
                          {{ seg.churn | number:'1.0-2' }}%
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- Visual Bar Indicators for Chart Trends -->
          <div class="charts-row">
            <div class="glass-card" *ngIf="dashboardData.charts.arpuByAccountType">
              <h3>ARPU by Account Type Trend</h3>
              <div class="chart-bars mt-16">
                <div class="bar-item mt-12" *ngFor="let label of dashboardData.charts.arpuByAccountType.labels; let idx = index">
                  <div class="bar-meta">
                    <span class="font-bold">{{ label }}</span>
                    <span>\${{ dashboardData.charts.arpuByAccountType.values?.[idx] | number:'1.2-2' }}</span>
                  </div>
                  <div class="progress-bar-bg mt-4">
                    <!-- scale percentage based on max value -->
                    <div class="progress-bar-fill progress-bar-success" [style.width.%]="(dashboardData.charts.arpuByAccountType.values?.[idx] || 0) * 2"></div>
                  </div>
                </div>
              </div>
            </div>

            <div class="glass-card" *ngIf="dashboardData.charts.collectionOverdueAgeing">
              <h3>Collection Overdue Aging</h3>
              <div class="chart-bars mt-16">
                <div class="bar-item mt-12" *ngFor="let label of dashboardData.charts.collectionOverdueAgeing.labels; let idx = index">
                  <div class="bar-meta">
                    <span class="font-bold">{{ label }}</span>
                    <span>\${{ dashboardData.charts.collectionOverdueAgeing.values?.[idx] | number:'1.2-2' }}</span>
                  </div>
                  <div class="progress-bar-bg mt-4">
                    <div class="progress-bar-fill progress-bar-danger" [style.width.%]="(dashboardData.charts.collectionOverdueAgeing.values?.[idx] || 0) / 10"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Saved Historical Snapshots Tab -->
      <div *ngIf="activeTab === 'snapshots'" class="tab-content animate-fade-in">
        <div class="snapshots-layout">
          <!-- Catalog List -->
          <div class="glass-card list-panel">
            <h3>Saved Reports snapshots</h3>
            <div class="table-container mt-16">
              <table class="table">
                <thead>
                  <tr>
                    <th>Report ID</th>
                    <th>Scope</th>
                    <th>Value</th>
                    <th>From Date</th>
                    <th>To Date</th>
                    <th>Generated Date</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let report of reportsList">
                    <td><code>#{{ report.reportId }}</code></td>
                    <td><span class="badge badge-info">{{ report.scope }}</span></td>
                    <td><strong>{{ report.scopeValue }}</strong></td>
                    <td>{{ report.periodStart }}</td>
                    <td>{{ report.periodEnd }}</td>
                    <td>{{ report.generatedDate | date:'mediumDate' }}</td>
                    <td class="action-cell">
                      <button (click)="downloadSavedReport(report.reportId, 'pdf')" class="btn btn-secondary btn-small">PDF</button>
                      <button (click)="downloadSavedReport(report.reportId, 'csv')" class="btn btn-secondary btn-small">CSV</button>
                    </td>
                  </tr>
                  <tr *ngIf="reportsList.length === 0">
                    <td colspan="7" class="text-center muted-text py-20">No report snapshots generated.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Generate snapshot card -->
          <div class="glass-card snapshot-panel">
            <h3>Generate Snapshot Report</h3>
            <p class="description mb-16">Trigger on-demand executive snapshot report to store permanently in history database.</p>
            
            <form [formGroup]="snapshotForm" (ngSubmit)="onGenerateSnapshot()" class="mt-16">
              <div class="form-group">
                <label class="form-label">Report Scope</label>
                <select formControlName="scope" class="form-control form-select">
                  <option value="REGION">REGION</option>
                  <option value="PLAN">PLAN</option>
                  <option value="SEGMENT">SEGMENT</option>
                  <option value="PERIOD">PERIOD</option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Scope Filter Value</label>
                <input type="text" formControlName="scopeValue" class="form-control" placeholder="e.g. South, Postpaid, Individual, ALL">
              </div>

              <div class="form-group">
                <label class="form-label">Period Start Date</label>
                <input type="date" formControlName="periodStart" class="form-control">
              </div>

              <div class="form-group">
                <label class="form-label">Period End Date</label>
                <input type="date" formControlName="periodEnd" class="form-control">
              </div>

              <button type="submit" [disabled]="snapshotForm.invalid" class="btn btn-primary btn-block mt-12">
                Generate Snapshot Report
              </button>
            </form>
          </div>
        </div>
      </div>

      <!-- Live Ad-Hoc Calculators Tab -->
      <div *ngIf="activeTab === 'adhoc'" class="tab-content animate-fade-in">
        <div class="adhoc-grid">
          <!-- ARPU Live Calculator -->
          <div class="glass-card">
            <h3>ARPU Calculator</h3>
            <form [formGroup]="arpuForm" (ngSubmit)="computeARPU()" class="mt-16">
              <div class="form-group">
                <label class="form-label">Billing Cycle ID</label>
                <input type="number" formControlName="cycleId" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">Scope</label>
                <select formControlName="scope" class="form-control form-select">
                  <option value="PERIOD">PERIOD</option>
                  <option value="REGION">REGION</option>
                  <option value="SEGMENT">SEGMENT</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Scope Value</label>
                <input type="text" formControlName="scopeValue" class="form-control">
              </div>
              <div class="calculator-actions mt-12">
                <button type="submit" [disabled]="arpuForm.invalid" class="btn btn-primary">Compute</button>
                <button type="button" (click)="downloadArpuReport()" class="btn btn-secondary">PDF</button>
              </div>
            </form>

            <div *ngIf="arpuRes" class="calculator-result mt-20 animate-fade-in">
              <div class="report-meta-grid">
                <div class="meta-item">
                  <span class="label">ARPU Value:</span>
                  <span class="value font-bold text-success">\${{ arpuRes.arpuValue | number:'1.2-2' }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Total Subscribers:</span>
                  <span class="value">{{ arpuRes.totalSubscribers }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Total Revenue:</span>
                  <span class="value font-bold">\${{ arpuRes.totalRevenue | number:'1.2-2' }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Churn Live Calculator -->
          <div class="glass-card">
            <h3>Churn Rate Calculator</h3>
            <form [formGroup]="churnForm" (ngSubmit)="computeChurn()" class="mt-16">
              <div class="form-group">
                <label class="form-label">Period Start Date</label>
                <input type="date" formControlName="periodStart" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">Period End Date</label>
                <input type="date" formControlName="periodEnd" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">Region (Optional)</label>
                <input type="text" formControlName="region" class="form-control">
              </div>
              <div class="calculator-actions mt-12">
                <button type="submit" [disabled]="churnForm.invalid" class="btn btn-primary">Compute</button>
                <button type="button" (click)="downloadChurnReport()" class="btn btn-secondary">PDF</button>
              </div>
            </form>

            <div *ngIf="churnRes" class="calculator-result mt-20 animate-fade-in">
              <div class="report-meta-grid">
                <div class="meta-item">
                  <span class="label">Churn Rate:</span>
                  <span class="value font-bold text-danger">{{ churnRes.churnRate | number:'1.0-2' }}%</span>
                </div>
                <div class="meta-item">
                  <span class="label">Subscribers Lost:</span>
                  <span class="value">{{ churnRes.subscribersLost }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Starting count:</span>
                  <span class="value">{{ churnRes.subscribersAtStart }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Network Utilisation Calculator -->
          <div class="glass-card">
            <h3>Network Utilisation</h3>
            <form [formGroup]="networkForm" (ngSubmit)="computeNetwork()" class="mt-16">
              <div class="form-group">
                <label class="form-label">Billing Cycle ID</label>
                <input type="number" formControlName="cycleId" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">Region (Optional)</label>
                <input type="text" formControlName="region" class="form-control">
              </div>
              <div class="calculator-actions mt-12">
                <button type="submit" [disabled]="networkForm.invalid" class="btn btn-primary">Compute</button>
                <button type="button" (click)="downloadNetworkReport()" class="btn btn-secondary">PDF</button>
              </div>
            </form>

            <div *ngIf="networkRes" class="calculator-result mt-20 animate-fade-in">
              <div class="report-meta-grid">
                <div class="meta-item">
                  <span class="label">Overall Utilisation:</span>
                  <span class="value font-bold text-success">{{ networkRes.overallUtilisationPercentage | number:'1.0-2' }}%</span>
                </div>
                <div class="meta-item">
                  <span class="label">Total Consumed (MB):</span>
                  <span class="value">{{ networkRes.totalDataConsumedMb | number }} MB</span>
                </div>
                <div class="meta-item">
                  <span class="label">Total Capacity (MB):</span>
                  <span class="value">{{ networkRes.totalDataCapacityMb | number }} MB</span>
                </div>
              </div>
            </div>
          </div>

          <!-- SLA Compliance Calculator -->
          <div class="glass-card">
            <h3>SLA Compliance</h3>
            <form [formGroup]="slaForm" (ngSubmit)="computeSLA()" class="mt-16">
              <div class="form-group">
                <label class="form-label">Period Start Date</label>
                <input type="date" formControlName="periodStart" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">Period End Date</label>
                <input type="date" formControlName="periodEnd" class="form-control">
              </div>
              <div class="calculator-actions mt-12">
                <button type="submit" [disabled]="slaForm.invalid" class="btn btn-primary">Compute</button>
                <button type="button" (click)="downloadSlaReport()" class="btn btn-secondary">PDF</button>
              </div>
            </form>

            <div *ngIf="slaRes" class="calculator-result mt-20 animate-fade-in">
              <div class="report-meta-grid">
                <div class="meta-item">
                  <span class="label">SLA Compliance:</span>
                  <span class="value font-bold text-success">{{ slaRes.slaCompliancePercentage | number:'1.0-2' }}%</span>
                </div>
                <div class="meta-item">
                  <span class="label">Tickets In SLA:</span>
                  <span class="value text-success">{{ slaRes.ticketsResolvedInSLA }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Total Tickets Raised:</span>
                  <span class="value">{{ slaRes.totalTicketsRaised }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .analytics-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .analytics-header h1 {
      font-size: 2.2rem;
      margin-bottom: 6px;
    }
    .analytics-header p {
      color: var(--text-secondary);
      font-size: 1rem;
    }
    .filter-layout {
      display: flex;
      align-items: flex-end;
      gap: 16px;
      flex-wrap: wrap;
    }
    .filter-actions {
      display: flex;
      gap: 12px;
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
    .kpi-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
      gap: 20px;
    }
    .kpi-card {
      text-align: center;
    }
    .kpi-card h4 {
      font-size: 0.8rem;
      color: var(--text-secondary);
      letter-spacing: 0.05em;
    }
    .huge-number {
      font-size: 2rem;
      font-weight: 800;
    }
    .analysis-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }
    @media (max-width: 992px) {
      .analysis-grid {
        grid-template-columns: 1fr;
      }
    }
    .charts-row {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }
    @media (max-width: 992px) {
      .charts-row {
        grid-template-columns: 1fr;
      }
    }
    .progress-bar-bg {
      background: var(--bg-tertiary);
      border-radius: 999px;
      height: 8px;
      overflow: hidden;
    }
    .progress-bar-fill {
      height: 100%;
      border-radius: 999px;
    }
    .progress-bar-success {
      background: var(--success);
    }
    .progress-bar-danger {
      background: var(--danger);
    }
    .bar-meta {
      display: flex;
      justify-content: space-between;
      font-size: 0.85rem;
    }
    .snapshots-layout {
      display: grid;
      grid-template-columns: 1fr 340px;
      gap: 24px;
    }
    @media (max-width: 992px) {
      .snapshots-layout {
        grid-template-columns: 1fr;
      }
    }
    .adhoc-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 24px;
    }
    .report-meta-grid {
      display: flex;
      flex-direction: column;
      gap: 12px;
      background: var(--bg-tertiary);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 16px;
    }
    .meta-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .meta-item .label {
      color: var(--text-secondary);
      font-size: 0.85rem;
    }
    .meta-item .value {
      font-weight: 500;
    }
    .calculator-actions {
      display: flex;
      gap: 8px;
    }
  `]
})
export class AnalyticsDashboardComponent implements OnInit {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly userService = inject(UserService);
  readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  activeTab = 'dashboard';
  currentUserId!: number;

  // Filter params
  cycleId = 1;
  startDate = '';
  endDate = '';

  // Data
  dashboardData: DashboardResponse | null = null;
  reportsList: TelecomReportResponse[] = [];

  // Live calculator responses
  arpuRes: any = null;
  churnRes: any = null;
  networkRes: any = null;
  slaRes: any = null;

  // Forms
  snapshotForm!: FormGroup;
  arpuForm!: FormGroup;
  churnForm!: FormGroup;
  networkForm!: FormGroup;
  slaForm!: FormGroup;

  // Alerts
  alertMessage = '';
  successMessage = '';
  errorMessage = '';

  ngOnInit() {
    this.initDefaultDates();
    this.initForms();
    this.loadDashboard();
    this.loadCurrentUserId();
  }

  setTab(tab: string) {
    this.activeTab = tab;
    this.clearAlerts();
    if (tab === 'dashboard') this.loadDashboard();
    if (tab === 'snapshots') this.loadSnapshots();
  }

  private initDefaultDates() {
    const end = new Date();
    const start = new Date();
    start.setMonth(start.getMonth() - 1);

    this.endDate = end.toISOString().split('T')[0];
    this.startDate = start.toISOString().split('T')[0];
  }

  private initForms() {
    this.snapshotForm = this.fb.group({
      scope: ['REGION', Validators.required],
      scopeValue: ['South', Validators.required],
      periodStart: [this.startDate, Validators.required],
      periodEnd: [this.endDate, Validators.required]
    });

    this.arpuForm = this.fb.group({
      cycleId: [1, Validators.required],
      scope: ['PERIOD', Validators.required],
      scopeValue: ['ALL', Validators.required]
    });

    this.churnForm = this.fb.group({
      periodStart: [this.startDate, Validators.required],
      periodEnd: [this.endDate, Validators.required],
      region: ['']
    });

    this.networkForm = this.fb.group({
      cycleId: [1, Validators.required],
      region: ['']
    });

    this.slaForm = this.fb.group({
      periodStart: [this.startDate, Validators.required],
      periodEnd: [this.endDate, Validators.required]
    });
  }

  private clearAlerts() {
    this.alertMessage = '';
    this.successMessage = '';
    this.errorMessage = '';
  }

  private loadCurrentUserId() {
    this.userService.getMe().subscribe({
      next: (u) => this.currentUserId = u.userId
    });
  }

  loadDashboard() {
    this.clearAlerts();
    this.analyticsService.getDashboard(this.startDate, this.endDate, this.cycleId).subscribe({
      next: (res) => this.dashboardData = res,
      error: () => this.errorMessage = 'Failed to load executive dashboard aggregates.'
    });
  }

  loadSnapshots() {
    this.clearAlerts();
    this.analyticsService.listReports().subscribe({
      next: (page) => this.reportsList = page.content || [],
      error: () => this.reportsList = []
    });
  }

  // Dashboard PDF export
  downloadDashboardPdf() {
    this.analyticsService.exportDashboard(this.cycleId, this.startDate, this.endDate).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `executive-dashboard-cycle-${this.cycleId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.errorMessage = 'Failed to export dashboard PDF.'
    });
  }

  // Stored snapshots
  onGenerateSnapshot() {
    if (this.snapshotForm.invalid) return;
    const req: ReportGenerationRequest = {
      ...this.snapshotForm.value,
      generatedBy: this.currentUserId
    };

    this.analyticsService.generateReport(req).subscribe({
      next: () => {
        this.successMessage = 'Snapshot report generated and stored in catalog!';
        this.loadSnapshots();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to generate report snapshot.'
    });
  }

  downloadSavedReport(reportId: number, format = 'pdf') {
    this.analyticsService.exportSavedReport(reportId, format).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Report_${reportId}.${format}`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.errorMessage = `Failed to download report snapshot in ${format} format.`
    });
  }

  // Ad-hoc live computations
  computeARPU() {
    if (this.arpuForm.invalid) return;
    const { cycleId, scope, scopeValue } = this.arpuForm.value;
    this.analyticsService.getARPU(cycleId, scope, scopeValue).subscribe({
      next: (res) => this.arpuRes = res,
      error: () => this.arpuRes = null
    });
  }

  downloadArpuReport() {
    if (this.arpuForm.invalid) return;
    const { cycleId, scope, scopeValue } = this.arpuForm.value;
    this.analyticsService.exportArpu(cycleId, scope, scopeValue).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `ARPU_Report_${cycleId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  computeChurn() {
    if (this.churnForm.invalid) return;
    const { periodStart, periodEnd, region } = this.churnForm.value;
    this.analyticsService.getChurn(periodStart, periodEnd, region || undefined).subscribe({
      next: (res) => this.churnRes = res,
      error: () => this.churnRes = null
    });
  }

  downloadChurnReport() {
    if (this.churnForm.invalid) return;
    const { periodStart, periodEnd, region } = this.churnForm.value;
    this.analyticsService.exportChurn(periodStart, periodEnd, region || undefined).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Churn_Report.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  computeNetwork() {
    if (this.networkForm.invalid) return;
    const { cycleId, region } = this.networkForm.value;
    this.analyticsService.getNetworkUtilisation(cycleId, region || undefined).subscribe({
      next: (res) => this.networkRes = res,
      error: () => this.networkRes = null
    });
  }

  downloadNetworkReport() {
    if (this.networkForm.invalid) return;
    const { cycleId, region } = this.networkForm.value;
    this.analyticsService.exportNetworkUtilisation(cycleId, region || undefined).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Network_Utilisation_Report.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  computeSLA() {
    if (this.slaForm.invalid) return;
    const { periodStart, periodEnd } = this.slaForm.value;
    this.analyticsService.getSLACompliance(periodStart, periodEnd).subscribe({
      next: (res) => this.slaRes = res,
      error: () => this.slaRes = null
    });
  }

  downloadSlaReport() {
    if (this.slaForm.invalid) return;
    const { periodStart, periodEnd } = this.slaForm.value;
    this.analyticsService.exportSLACompliance(periodStart, periodEnd).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `SLA_Compliance_Report.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  getRegionsList(regions: RegionalAnalysis): Array<{ name: string, subscribers: number, churn: number, arpu: number }> {
    if (!regions || !regions.subscribersByRegion) return [];
    return Object.keys(regions.subscribersByRegion).map(key => ({
      name: key,
      subscribers: regions.subscribersByRegion[key],
      churn: regions.churnByRegion[key] || 0,
      arpu: regions.arpuByRegion[key] || 0
    }));
  }

  getSegmentsList(segments: SegmentAnalysis): Array<{ name: string, subscribers: number, churn: number, arpu: number }> {
    if (!segments || !segments.subscribersByAccountType) return [];
    return Object.keys(segments.subscribersByAccountType).map(key => ({
      name: key,
      subscribers: segments.subscribersByAccountType[key],
      churn: segments.churnByAccountType[key] || 0,
      arpu: segments.arpuByAccountType[key] || 0
    }));
  }
}
