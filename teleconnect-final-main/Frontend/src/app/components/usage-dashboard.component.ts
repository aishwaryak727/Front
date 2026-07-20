import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { UsageService, UsageRecordResponse, UsageSummaryResponse, LimitStatusResponse, AlertResponse, AnalyticsTrendResponse } from '../services/usage.service';
import { SubscriberService, AccountResponse, SimLineResponse } from '../services/subscriber.service';
import { PlanService, TelecomPlanResponse } from '../services/plan.service';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-usage-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="usage-container animate-fade-in">
      <header class="usage-header">
        <h1>Usage Tracking & Analytics</h1>
        <p>Monitor real-time data, voice, and SMS consumption, remaining quotas, and threshold alerts</p>
      </header>

      <div class="usage-grid">
        <!-- LEFT COLUMN: Selector & Info -->
        <div class="left-panel">
          <!-- SIM Selection Card -->
          <div class="glass-card mb-24">
            <h3>Select Active SIM Line</h3>
            <p class="muted-text mt-4">Select or search for a SIM card line to check current usage logs.</p>
            
            <!-- Subscriber Selection Dropdown -->
            <div class="form-group mt-16" *ngIf="isSubscriber">
              <label class="form-label">Active Lines</label>
              <select [(ngModel)]="selectedLineId" (change)="onLineChange()" class="form-control form-select">
                <option value="" disabled>-- Select a Line --</option>
                <option *ngFor="let sim of subscriberLines" [value]="sim.lineId">
                  {{ sim.msisdn }} - {{ sim.serviceType }} (Line #{{ sim.lineId }})
                </option>
              </select>
            </div>

            <!-- Staff/Admin Search Input -->
            <div class="staff-search mt-16" *ngIf="!isSubscriber">
              <div class="form-group">
                <label class="form-label">Search Line by Phone Number (MSISDN)</label>
                <div class="search-group">
                  <input type="text" [(ngModel)]="searchMsisdn" placeholder="e.g. +1234567890" class="form-control">
                  <button (click)="searchLine()" class="btn btn-primary">Search</button>
                </div>
              </div>
              
              <div *ngIf="foundLine" class="found-line-info mt-12 animate-fade-in">
                <div class="meta-item">
                  <span class="label">Line ID:</span>
                  <span class="value">#{{ foundLine.lineId }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Status:</span>
                  <span class="badge badge-success">{{ foundLine.status }}</span>
                </div>
              </div>
            </div>

            <!-- Billing Cycle Picker -->
            <div class="form-group mt-16">
              <label class="form-label">Billing Cycle ID</label>
              <input type="number" [(ngModel)]="billingCycleId" (change)="onCycleChange()" class="form-control" placeholder="e.g. 1">
            </div>
          </div>

          <!-- Active Plan Limits Card -->
          <div class="glass-card mb-24 animate-fade-in" *ngIf="activePlan">
            <h3>Active Subscription Plan</h3>
            <div class="plan-details mt-16">
              <div class="plan-title-row">
                <h4>{{ activePlan.name }}</h4>
                <span class="badge badge-info">{{ activePlan.type }}</span>
              </div>
              <div class="limits-meta mt-12">
                <div class="meta-item">
                  <span class="label">Data Quota:</span>
                  <span class="value font-bold">{{ activePlan.dataGb }} GB</span>
                </div>
                <div class="meta-item">
                  <span class="label">Voice Limit:</span>
                  <span class="value font-bold">{{ activePlan.voiceMinutes === -1 ? 'Unlimited' : activePlan.voiceMinutes + ' Mins' }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">SMS Limit:</span>
                  <span class="value font-bold">{{ activePlan.smsCount === -1 ? 'Unlimited' : activePlan.smsCount + ' SMS' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- RIGHT COLUMN: Quota Progress, Alerts, Logs, Analytics -->
        <div class="right-panel">
          <div *ngIf="!selectedLineId" class="glass-card text-center py-40">
            <span class="huge-icon">📱</span>
            <h2>No SIM Line Selected</h2>
            <p class="muted-text mt-8">Please choose or search for an active line to inspect usage details.</p>
          </div>

          <div *ngIf="selectedLineId" class="animate-fade-in">
            <!-- Tabs inside usage view -->
            <div class="sub-tabs mb-20">
              <button [class.active]="activeSubTab === 'quota'" (click)="setSubTab('quota')" class="sub-tab-btn">
                📊 Quota Progress & Alerts
              </button>
              <button [class.active]="activeSubTab === 'logs'" (click)="setSubTab('logs')" class="sub-tab-btn">
                📜 Usage History Logs
              </button>
              <button *ngIf="hasAnalyticsPermission" [class.active]="activeSubTab === 'analytics'" (click)="setSubTab('analytics')" class="sub-tab-btn">
                📈 Usage Trends & Insights
              </button>
              <button *ngIf="canSimulate" [class.active]="activeSubTab === 'simulate'" (click)="setSubTab('simulate')" class="sub-tab-btn">
                ⚡ Simulate Events
              </button>
            </div>

            <!-- Alerts / Messages -->
            <div *ngIf="alertMessage" class="alert alert-info mb-16">
              ℹ️ {{ alertMessage }}
            </div>
            <div *ngIf="successMessage" class="alert alert-success mb-16">
              🎉 {{ successMessage }}
            </div>
            <div *ngIf="errorMessage" class="alert alert-danger mb-16">
              ⚠️ {{ errorMessage }}
            </div>

            <!-- Quota & Alerts Tab -->
            <div *ngIf="activeSubTab === 'quota'" class="animate-fade-in">
              <!-- Threshold Warnings banner -->
              <div class="threshold-warnings mb-20" *ngIf="alertStatus">
                <div class="warning-banner" *ngIf="alertStatus.dataAlert !== 'OK'">
                  ⚠️ <strong>DATA WARNING:</strong> Data usage has crossed threshold levels (Status: {{ alertStatus.dataAlert }}).
                </div>
                <div class="warning-banner" *ngIf="alertStatus.voiceAlert !== 'OK'">
                  ⚠️ <strong>VOICE WARNING:</strong> Voice calls usage has crossed threshold levels (Status: {{ alertStatus.voiceAlert }}).
                </div>
                <div class="warning-banner" *ngIf="alertStatus.smsAlert !== 'OK'">
                  ⚠️ <strong>SMS WARNING:</strong> SMS messages usage has crossed threshold levels (Status: {{ alertStatus.smsAlert }}).
                </div>
              </div>

              <!-- Quota Cards Grid -->
              <div class="quota-grid">
                <!-- DATA Card -->
                <div class="glass-card quota-card">
                  <div class="quota-header">
                    <h4>📶 DATA CONSUMPTION</h4>
                    <span class="badge" [ngClass]="alertStatus?.dataAlert === 'OK' ? 'badge-success' : 'badge-danger'">
                      {{ alertStatus?.dataAlert || 'OK' }}
                    </span>
                  </div>
                  <div class="quota-progress-container mt-20">
                    <div class="progress-bar-bg">
                      <div class="progress-bar-fill" [style.width.%]="limitStatus?.dataPercentage || 0" [ngClass]="getProgressBarClass(limitStatus?.dataPercentage || 0)"></div>
                    </div>
                    <div class="progress-meta mt-12">
                      <span>Used: {{ limitStatus?.dataUsedMb | number:'1.0-2' }} MB</span>
                      <span>Limit: {{ limitStatus?.dataLimitMb | number:'1.0-2' }} MB</span>
                    </div>
                    <div class="percent-label mt-8 font-bold">{{ limitStatus?.dataPercentage || 0 | number:'1.0-0' }}% Consumed</div>
                  </div>
                </div>

                <!-- VOICE Card -->
                <div class="glass-card quota-card">
                  <div class="quota-header">
                    <h4>📞 VOICE CALLS</h4>
                    <span class="badge" [ngClass]="alertStatus?.voiceAlert === 'OK' ? 'badge-success' : 'badge-danger'">
                      {{ alertStatus?.voiceAlert || 'OK' }}
                    </span>
                  </div>
                  <div class="quota-progress-container mt-20">
                    <div class="progress-bar-bg">
                      <div class="progress-bar-fill" [style.width.%]="limitStatus?.voicePercentage || 0" [ngClass]="getProgressBarClass(limitStatus?.voicePercentage || 0)"></div>
                    </div>
                    <div class="progress-meta mt-12">
                      <span>Used: {{ limitStatus?.voiceUsedMin | number:'1.0-2' }} Mins</span>
                      <span>Limit: {{ limitStatus?.voiceLimitMin | number:'1.0-2' }} Mins</span>
                    </div>
                    <div class="percent-label mt-8 font-bold">{{ limitStatus?.voicePercentage || 0 | number:'1.0-0' }}% Consumed</div>
                  </div>
                </div>

                <!-- SMS Card -->
                <div class="glass-card quota-card">
                  <div class="quota-header">
                    <h4>✉️ SMS MESSAGES</h4>
                    <span class="badge" [ngClass]="alertStatus?.smsAlert === 'OK' ? 'badge-success' : 'badge-danger'">
                      {{ alertStatus?.smsAlert || 'OK' }}
                    </span>
                  </div>
                  <div class="quota-progress-container mt-20">
                    <div class="progress-bar-bg">
                      <div class="progress-bar-fill" [style.width.%]="limitStatus?.smsPercentage || 0" [ngClass]="getProgressBarClass(limitStatus?.smsPercentage || 0)"></div>
                    </div>
                    <div class="progress-meta mt-12">
                      <span>Used: {{ limitStatus?.smsUsed }} SMS</span>
                      <span>Limit: {{ limitStatus?.smsLimit }} SMS</span>
                    </div>
                    <div class="percent-label mt-8 font-bold">{{ limitStatus?.smsPercentage || 0 | number:'1.0-0' }}% Consumed</div>
                  </div>
                </div>
              </div>

              <!-- Detailed Summary info -->
              <div class="glass-card mt-24" *ngIf="usageSummary">
                <h3>Usage Quota Summary</h3>
                <div class="summary-grid mt-16">
                  <div class="summary-item">
                    <span class="label">Remaining Data:</span>
                    <span class="value font-bold text-success">{{ usageSummary.dataRemainingMb | number:'1.0-2' }} MB</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">Remaining Voice:</span>
                    <span class="value font-bold text-success">{{ usageSummary.voiceRemainingMin | number:'1.0-2' }} Mins</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">Remaining SMS:</span>
                    <span class="value font-bold text-success">{{ usageSummary.smsRemaining }} SMS</span>
                  </div>
                  <div class="summary-item">
                    <span class="label">Last Consumption Sync:</span>
                    <span class="value">{{ usageSummary.lastUpdated | date:'medium' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Usage Logs Tab -->
            <div *ngIf="activeSubTab === 'logs'" class="tab-content animate-fade-in">
              <div class="glass-card">
                <h3>Raw Usage Session Logs</h3>
                <div class="table-container mt-16">
                  <table class="table">
                    <thead>
                      <tr>
                        <th>Log ID</th>
                        <th>Usage Type</th>
                        <th>Consumption Quantity</th>
                        <th>Session Date & Time</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr *ngFor="let log of usageLogs">
                        <td><code>#{{ log.recordId }}</code></td>
                        <td>
                          <span class="badge" [ngClass]="log.usageType === 'DATA' ? 'badge-info' : (log.usageType === 'VOICE' ? 'badge-success' : 'badge-warning')">
                            {{ log.usageType }}
                          </span>
                        </td>
                        <td>
                          <strong>{{ log.quantity }}</strong> {{ log.unit }}
                        </td>
                        <td>{{ log.usageDate | date:'medium' }}</td>
                      </tr>
                      <tr *ngIf="usageLogs.length === 0">
                        <td colspan="4" class="text-center muted-text py-20">No usage logs found for this line.</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- Advanced Trends & Analytics -->
            <div *ngIf="activeSubTab === 'analytics' && hasAnalyticsPermission" class="tab-content animate-fade-in">
              <div class="analytics-row">
                <div class="glass-card metric-card">
                  <h4>TOTAL TRACKED EVENTS</h4>
                  <div class="huge-number mt-8">{{ trendStats?.totalRecordsCount || 0 }}</div>
                </div>
                <div class="glass-card metric-card">
                  <h4>AVERAGE SESSION DATA</h4>
                  <div class="huge-number mt-8">{{ trendStats?.averageDataUsedMb || 0 | number:'1.0-2' }} MB</div>
                </div>
                <div class="glass-card metric-card">
                  <h4>AVERAGE VOICE CALLS</h4>
                  <div class="huge-number mt-8">{{ trendStats?.averageVoiceUsedMin || 0 | number:'1.0-2' }} Mins</div>
                </div>
              </div>

              <!-- Top usages category list -->
              <div class="glass-card mt-24" *ngIf="topUsageData">
                <h3>Category Breakdown & Top Usage Sessions</h3>
                <div class="meta-grid mt-16">
                  <div class="meta-item">
                    <span class="label">Top Category:</span>
                    <span class="value font-bold">{{ topUsageData.topType || 'N/A' }}</span>
                  </div>
                  <div class="meta-item">
                    <span class="label">Total Logs:</span>
                    <span class="value">{{ topUsageData.totalRecords }} sessions</span>
                  </div>
                </div>

                <div class="table-container mt-20" *ngIf="topUsageData.topRecords">
                  <table class="table">
                    <thead>
                      <tr>
                        <th>Log ID</th>
                        <th>Type</th>
                        <th>Quantity</th>
                        <th>Date</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr *ngFor="let r of topUsageData.topRecords">
                        <td><code>#{{ r.recordId }}</code></td>
                        <td><span class="badge badge-secondary">{{ r.usageType }}</span></td>
                        <td><strong>{{ r.quantity }}</strong> {{ r.unit }}</td>
                        <td>{{ r.usageDate | date:'mediumDate' }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <!-- Simulate Events Tab -->
            <div *ngIf="activeSubTab === 'simulate' && canSimulate" class="tab-content animate-fade-in">
              <div class="glass-card max-width-600">
                <h3>Simulate Live Network Consumption</h3>
                <p class="description mb-20">Directly post a simulated network resource consumption event to this line.</p>

                <form [formGroup]="simulateForm" (ngSubmit)="onSubmitSimulation()" class="simulate-form">
                  <div class="form-group">
                    <label class="form-label">Resource Usage Type</label>
                    <select formControlName="usageType" class="form-control form-select">
                      <option value="DATA">DATA (Web browsing/streaming)</option>
                      <option value="VOICE">VOICE (Voice calls)</option>
                      <option value="SMS">SMS (Text messaging)</option>
                    </select>
                  </div>

                  <div class="form-group">
                    <label class="form-label">Consumption Quantity</label>
                    <input type="number" step="0.01" formControlName="quantity" class="form-control" placeholder="e.g. 50 MB, 10 Mins, 1 SMS">
                  </div>

                  <div class="form-group">
                    <label class="form-label">Simulate Timestamp</label>
                    <input type="datetime-local" formControlName="usageDate" class="form-control">
                  </div>

                  <button type="submit" [disabled]="simulateForm.invalid" class="btn btn-primary mt-12">
                    Inject Simulated Usage Event
                  </button>
                </form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .usage-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .usage-header h1 {
      font-size: 2.2rem;
      margin-bottom: 6px;
    }
    .usage-header p {
      color: var(--text-secondary);
      font-size: 1rem;
    }
    .usage-grid {
      display: grid;
      grid-template-columns: 320px 1fr;
      gap: 32px;
    }
    @media (max-width: 992px) {
      .usage-grid {
        grid-template-columns: 1fr;
      }
    }
    .search-group {
      display: flex;
      gap: 8px;
    }
    .found-line-info {
      background: var(--bg-tertiary);
      border-radius: var(--radius-md);
      padding: 12px;
      border: 1px solid var(--border-color);
    }
    .meta-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }
    .meta-item:last-child {
      margin-bottom: 0;
    }
    .meta-item .label {
      color: var(--text-secondary);
      font-size: 0.85rem;
    }
    .meta-item .value {
      font-weight: 500;
    }
    .sub-tabs {
      display: flex;
      gap: 8px;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 8px;
      overflow-x: auto;
    }
    .sub-tab-btn {
      background: transparent;
      border: none;
      color: var(--text-secondary);
      font-size: 0.9rem;
      font-weight: 600;
      padding: 8px 16px;
      cursor: pointer;
      border-radius: var(--radius-md);
      transition: var(--transition-smooth);
      white-space: nowrap;
    }
    .sub-tab-btn:hover {
      color: white;
      background: rgba(255, 255, 255, 0.02);
    }
    .sub-tab-btn.active {
      color: var(--accent-primary);
      background: var(--accent-glow);
    }
    .threshold-warnings {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .warning-banner {
      background: rgba(239, 68, 68, 0.1);
      border: 1px solid rgba(239, 68, 68, 0.3);
      color: #ef4444;
      padding: 12px 16px;
      border-radius: var(--radius-md);
      font-size: 0.9rem;
    }
    .quota-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 20px;
    }
    .quota-card {
      display: flex;
      flex-direction: column;
    }
    .quota-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .quota-header h4 {
      font-size: 0.85rem;
      letter-spacing: 0.05em;
      color: var(--text-secondary);
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
      transition: var(--transition-smooth);
    }
    .progress-bar-success {
      background: var(--success);
    }
    .progress-bar-warning {
      background: var(--warning);
    }
    .progress-bar-danger {
      background: var(--danger);
    }
    .progress-meta {
      display: flex;
      justify-content: space-between;
      font-size: 0.8rem;
      color: var(--text-secondary);
    }
    .percent-label {
      font-size: 0.9rem;
    }
    .summary-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
    }
    .summary-item {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .summary-item .label {
      color: var(--text-secondary);
      font-size: 0.8rem;
    }
    .summary-item .value {
      font-size: 1.1rem;
    }
    .analytics-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 20px;
    }
    .metric-card {
      text-align: center;
    }
    .metric-card h4 {
      font-size: 0.8rem;
      color: var(--text-secondary);
    }
    .huge-number {
      font-size: 2rem;
      font-weight: 800;
      color: var(--accent-primary);
    }
    .huge-icon {
      font-size: 3rem;
      display: block;
      margin-bottom: 12px;
    }
  `]
})
export class UsageDashboardComponent implements OnInit {
  private readonly usageService = inject(UsageService);
  private readonly subscriberService = inject(SubscriberService);
  private readonly planService = inject(PlanService);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  selectedLineId!: number;
  billingCycleId: number = 1;
  activeSubTab = 'quota';

  // Subscriber SIM list
  subscriberLines: SimLineResponse[] = [];

  // Staff Search
  searchMsisdn = '';
  foundLine: SimLineResponse | null = null;

  // Active limits
  activePlan: TelecomPlanResponse | null = null;

  // Loaded data
  usageLogs: UsageRecordResponse[] = [];
  usageSummary: UsageSummaryResponse | null = null;
  limitStatus: LimitStatusResponse | null = null;
  alertStatus: AlertResponse | null = null;
  trendStats: AnalyticsTrendResponse | null = null;
  topUsageData: any = null;

  // Simulate usage
  simulateForm!: FormGroup;

  // Feedback alerts
  alertMessage = '';
  successMessage = '';
  errorMessage = '';

  get isSubscriber(): boolean {
    return this.authService.userRole() === 'S';
  }

  get hasAnalyticsPermission(): boolean {
    return this.authService.hasPermission('USAGE_ANALYTICS');
  }

  get canSimulate(): boolean {
    return this.authService.hasPermission('CREATE_USER') || this.authService.userRole() === 'A';
  }

  ngOnInit() {
    this.initForm();
    this.loadSubscriberLines();
  }

  private initForm() {
    // default timestamp to now
    const now = new Date();
    const tzoffset = now.getTimezoneOffset() * 60000;
    const localISOTime = (new Date(now.getTime() - tzoffset)).toISOString().slice(0, 16);

    this.simulateForm = this.fb.group({
      usageType: ['DATA', Validators.required],
      quantity: [50, [Validators.required, Validators.min(0.01)]],
      usageDate: [localISOTime, Validators.required]
    });
  }

  private clearAlerts() {
    this.alertMessage = '';
    this.successMessage = '';
    this.errorMessage = '';
  }

  setSubTab(tab: string) {
    this.activeSubTab = tab;
    this.clearAlerts();
    this.loadSubTabDetails();
  }

  private loadSubscriberLines() {
    if (!this.isSubscriber) return;

    this.userService.getMe().subscribe({
      next: (user) => {
        this.subscriberService.getAllAccounts(undefined, user.userId).subscribe({
          next: (accountsRes) => {
            const accounts = accountsRes.subscribers;
            accounts.forEach(acc => {
              this.subscriberService.getSimLines(acc.accountId).subscribe({
                next: (lines) => {
                  this.subscriberLines.push(...lines.filter(l => l.status === 'Active'));
                  if (this.subscriberLines.length > 0 && !this.selectedLineId) {
                    this.selectedLineId = this.subscriberLines[0].lineId;
                    this.onLineChange();
                  }
                }
              });
            });
          }
        });
      }
    });
  }

  searchLine() {
    if (!this.searchMsisdn) {
      this.errorMessage = 'Please input a phone number.';
      return;
    }
    this.clearAlerts();
    this.subscriberService.lookupByMsisdn(this.searchMsisdn).subscribe({
      next: (sim) => {
        this.foundLine = sim;
        this.selectedLineId = sim.lineId;
        this.successMessage = `Found Line ID: #${sim.lineId}`;
        this.onLineChange();
      },
      error: () => {
        this.errorMessage = 'No SIM line found with this phone number.';
        this.foundLine = null;
      }
    });
  }

  onLineChange() {
    this.clearAlerts();
    this.activePlan = null;
    
    // Look up plan details from subscriber's subscription
    this.planService.getAllSubscriptions().subscribe({
      next: (subs) => {
        const activeSub = subs.find(s => s.lineId === Number(this.selectedLineId) && s.status === 'A');
        if (activeSub && activeSub.planId) {
          this.planService.getPlanById(activeSub.planId).subscribe({
            next: (plan) => {
              this.activePlan = plan;
              this.loadBaseDetails();
            }
          });
        } else {
          this.alertMessage = 'No active plan subscription found on this SIM line. Showing logs only.';
          this.loadBaseDetails();
        }
      },
      error: () => {
        this.loadBaseDetails();
      }
    });
  }

  onCycleChange() {
    this.onLineChange();
  }

  private loadBaseDetails() {
    if (!this.selectedLineId) return;

    // Fetch chronological raw records
    this.usageService.fetchRecords(Number(this.selectedLineId)).subscribe({
      next: (res) => this.usageLogs = res.records || [],
      error: () => this.usageLogs = []
    });

    this.loadSubTabDetails();
  }

  private loadSubTabDetails() {
    if (!this.selectedLineId) return;

    const dataLimit = this.activePlan ? this.activePlan.dataGb * 1024 : 5120;
    const voiceLimit = this.activePlan && this.activePlan.voiceMinutes !== -1 ? this.activePlan.voiceMinutes : 300;
    const smsLimit = this.activePlan && this.activePlan.smsCount !== -1 ? this.activePlan.smsCount : 100;

    if (this.activeSubTab === 'quota') {
      this.usageService.fetchSummary(Number(this.selectedLineId), this.billingCycleId).subscribe({
        next: (res) => this.usageSummary = res,
        error: () => this.usageSummary = null
      });

      this.usageService.getLimitStatus(Number(this.selectedLineId), this.billingCycleId, dataLimit, voiceLimit, smsLimit).subscribe({
        next: (res) => this.limitStatus = res,
        error: () => this.limitStatus = null
      });

      this.usageService.getAlerts(Number(this.selectedLineId), this.billingCycleId, dataLimit, voiceLimit, smsLimit).subscribe({
        next: (res) => this.alertStatus = res,
        error: () => this.alertStatus = null
      });
    }

    if (this.activeSubTab === 'analytics' && this.hasAnalyticsPermission) {
      this.usageService.getAnalyticsTrend(Number(this.selectedLineId)).subscribe({
        next: (res) => this.trendStats = res,
        error: () => this.trendStats = null
      });

      this.usageService.getTopUsage(Number(this.selectedLineId)).subscribe({
        next: (res) => this.topUsageData = res,
        error: () => this.topUsageData = null
      });
    }
  }

  getProgressBarClass(pct: number): string {
    if (pct < 70) return 'progress-bar-success';
    if (pct < 90) return 'progress-bar-warning';
    return 'progress-bar-danger';
  }

  // Simulate Inject Event
  onSubmitSimulation() {
    if (this.simulateForm.invalid || !this.selectedLineId) return;

    const dataLimit = this.activePlan ? this.activePlan.dataGb * 1024 : 5120;
    const voiceLimit = this.activePlan && this.activePlan.voiceMinutes !== -1 ? this.activePlan.voiceMinutes : 300;
    const smsLimit = this.activePlan && this.activePlan.smsCount !== -1 ? this.activePlan.smsCount : 100;

    const req = {
      lineId: Number(this.selectedLineId),
      billingCycleId: this.billingCycleId,
      usageType: this.simulateForm.value.usageType,
      quantity: Number(this.simulateForm.value.quantity),
      usageDate: new Date(this.simulateForm.value.usageDate).toISOString(),
      dataLimitMb: dataLimit,
      voiceLimitMin: voiceLimit,
      smsLimit: smsLimit
    };

    this.usageService.createRecord(req).subscribe({
      next: () => {
        this.successMessage = 'Network resource consumption event injected successfully!';
        this.loadBaseDetails();
        // Reset quantity
        this.simulateForm.patchValue({ quantity: 50 });
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to simulate event.'
    });
  }
}
