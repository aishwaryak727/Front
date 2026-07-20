import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { PlanService, TelecomPlanResponse, AddOnResponse, ServiceSubscriptionResponse } from '../services/plan.service';
import { SubscriberService, AccountResponse, SimLineResponse } from '../services/subscriber.service';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-plans-addons',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="plans-container animate-fade-in">
      <header class="plans-header">
        <h1>Telecom Plans & Add-Ons</h1>
        <p>Explore high-speed internet & voice plans, active subscriptions, and high-value add-ons</p>
      </header>

      <!-- Tabs Navigation -->
      <div class="tabs">
        <button [class.active]="activeTab === 'plans'" (click)="setTab('plans')" class="tab-btn">
          📋 Telecom Plans
        </button>
        <button [class.active]="activeTab === 'addons'" (click)="setTab('addons')" class="tab-btn">
          ⚡ Add-Ons
        </button>
        <button [class.active]="activeTab === 'subscriptions'" (click)="setTab('subscriptions')" class="tab-btn">
          🔑 Subscriptions
        </button>
        <button *ngIf="isAdmin" [class.active]="activeTab === 'manage'" (click)="setTab('manage')" class="tab-btn">
          🛠️ Create/Manage
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

      <!-- Telecom Plans Tab -->
      <div *ngIf="activeTab === 'plans'" class="tab-content animate-fade-in">
        <div class="plans-grid">
          <div *ngFor="let plan of plans" class="glass-card plan-card" [class.inactive-plan]="plan.status !== 'ACTIVE'">
            <div class="plan-badge-wrapper">
              <span class="badge" [ngClass]="plan.type === 'Prepaid' ? 'badge-info' : 'badge-success'">
                {{ plan.type }}
              </span>
              <span class="badge" [ngClass]="plan.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'">
                {{ plan.status }}
              </span>
            </div>
            <h2 class="plan-name">{{ plan.name }}</h2>
            <div class="plan-price">
              <span class="currency">$</span>
              <span class="amount">{{ plan.planPrice }}</span>
              <span class="period">/ {{ plan.validityDays }} Days</span>
            </div>
            
            <div class="plan-features mt-16">
              <div class="feature-item">
                <span class="icon font-bold">📶</span>
                <span>{{ plan.dataGb }} GB High Speed Data</span>
              </div>
              <div class="feature-item">
                <span class="icon">📞</span>
                <span>{{ plan.voiceMinutes === -1 ? 'Unlimited' : plan.voiceMinutes + ' Mins' }} Voice Calls</span>
              </div>
              <div class="feature-item">
                <span class="icon">✉️</span>
                <span>{{ plan.smsCount === -1 ? 'Unlimited' : plan.smsCount + ' SMS' }} SMS</span>
              </div>
            </div>

            <!-- Subscribe action for Subscribers -->
            <div class="plan-actions mt-24" *ngIf="isSubscriber && plan.status === 'ACTIVE'">
              <button (click)="openSubscribeModal(plan)" class="btn btn-primary btn-block">
                Subscribe Plan
              </button>
            </div>
            <!-- Edit action for Admins -->
            <div class="plan-actions mt-24" *ngIf="isAdmin">
              <button (click)="openEditPlanModal(plan)" class="btn btn-secondary btn-block">
                Edit Plan Settings
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Add-Ons Tab -->
      <div *ngIf="activeTab === 'addons'" class="tab-content animate-fade-in">
        <div class="plans-grid">
          <div *ngFor="let addon of addons" class="glass-card plan-card">
            <div class="plan-badge-wrapper">
              <span class="badge badge-info">{{ addon.type }}</span>
              <span class="badge" [ngClass]="addon.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'">
                {{ addon.status }}
              </span>
            </div>
            <h2 class="plan-name">{{ addon.name }}</h2>
            <div class="plan-price">
              <span class="currency">$</span>
              <span class="amount">{{ addon.price }}</span>
              <span class="period">/ {{ addon.validityDays }} Days</span>
            </div>

            <div class="plan-features mt-16">
              <div class="feature-item">
                <span class="icon">📦</span>
                <span>Quota: {{ addon.quota }}</span>
              </div>
            </div>

            <!-- Apply Addon for Subscribers -->
            <div class="plan-actions mt-24" *ngIf="isSubscriber && addon.status === 'ACTIVE'">
              <button (click)="openApplyAddonModal(addon)" class="btn btn-primary btn-block">
                Apply Add-On
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Subscriptions Tab -->
      <div *ngIf="activeTab === 'subscriptions'" class="tab-content animate-fade-in">
        <div class="glass-card">
          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Subscription ID</th>
                  <th>SIM Line ID</th>
                  <th>Plan Details</th>
                  <th>Add-On Details</th>
                  <th>Activation Date</th>
                  <th>Expiry Date</th>
                  <th>Renewal Type</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let sub of displayedSubscriptions">
                  <td><code>#{{ sub.subscriptionId }}</code></td>
                  <td><code>#{{ sub.lineId }}</code></td>
                  <td>
                    <strong>{{ getPlanName(sub.planId) }}</strong>
                    <div class="muted-text text-small" *ngIf="sub.planId">ID: {{ sub.planId }}</div>
                  </td>
                  <td>
                    <span *ngIf="sub.addOnId" class="badge badge-info">
                      {{ getAddonName(sub.addOnId) }}
                    </span>
                    <span *ngIf="!sub.addOnId" class="muted-text">-</span>
                  </td>
                  <td>{{ sub.activationDate }}</td>
                  <td>{{ sub.expiryDate }}</td>
                  <td>
                    <span class="badge badge-secondary">{{ sub.renewalType }}</span>
                  </td>
                  <td>
                    <span class="badge" [ngClass]="sub.status === 'A' ? 'badge-success' : (sub.status === 'E' ? 'badge-danger' : 'badge-warning')">
                      {{ sub.status === 'A' ? 'Active' : (sub.status === 'E' ? 'Expired' : 'Suspended') }}
                    </span>
                  </td>
                  <td class="action-cell">
                    <button *ngIf="isAdmin || (isSubscriber && sub.status === 'A')" 
                            (click)="openUpdateSubscriptionModal(sub)" 
                            class="btn btn-secondary btn-small">
                      🔄 Change
                    </button>
                  </td>
                </tr>
                <tr *ngIf="displayedSubscriptions.length === 0">
                  <td colspan="9" class="text-center muted-text py-20">No active subscriptions found.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Create/Manage Tab (Admin Only) -->
      <div *ngIf="activeTab === 'manage' && isAdmin" class="tab-content animate-fade-in">
        <div class="manage-grid">
          <!-- Create Plan Card -->
          <div class="glass-card">
            <h3>Create New Telecom Plan</h3>
            <form [formGroup]="planForm" (ngSubmit)="onCreatePlan()" class="mt-20">
              <div class="form-group">
                <label class="form-label">Plan Name</label>
                <input type="text" formControlName="name" class="form-control" placeholder="Super Data Unlimited">
              </div>
              <div class="form-group">
                <label class="form-label">Plan Type</label>
                <select formControlName="type" class="form-control form-select">
                  <option value="Prepaid">Prepaid</option>
                  <option value="Postpaid">Postpaid</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Data (GB)</label>
                <input type="number" formControlName="dataGb" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">Voice Minutes (-1 for Unlimited)</label>
                <input type="number" formControlName="voiceMinutes" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">SMS Count (-1 for Unlimited)</label>
                <input type="number" formControlName="smsCount" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">Validity (Days)</label>
                <input type="number" formControlName="validityDays" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">Price ($)</label>
                <input type="number" formControlName="planPrice" class="form-control">
              </div>
              <button type="submit" [disabled]="planForm.invalid" class="btn btn-primary mt-12">
                Create Plan
              </button>
            </form>
          </div>

          <!-- Create Add-on Card -->
          <div class="glass-card">
            <h3>Create New Add-On Pack</h3>
            <form [formGroup]="addonForm" (ngSubmit)="onCreateAddon()" class="mt-20">
              <div class="form-group">
                <label class="form-label">Add-On Name</label>
                <input type="text" formControlName="name" class="form-control" placeholder="ISD Booster 50">
              </div>
              <div class="form-group">
                <label class="form-label">Type</label>
                <select formControlName="type" class="form-control form-select">
                  <option value="DataTopup">DataTopup</option>
                  <option value="ISDPack">ISDPack</option>
                  <option value="RoamingPack">RoamingPack</option>
                  <option value="SMSPack">SMSPack</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">Quota</label>
                <input type="number" formControlName="quota" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">Validity (Days)</label>
                <input type="number" formControlName="validityDays" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">Price ($)</label>
                <input type="number" formControlName="price" class="form-control">
              </div>
              <button type="submit" [disabled]="addonForm.invalid" class="btn btn-primary mt-12">
                Create Add-On
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>

    <!-- Modals -->

    <!-- Subscription Modal -->
    <div *ngIf="showSubscribeModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Subscribe to {{ selectedPlan?.name }}</h2>
        <p class="muted-text mt-8">Choose one of your active SIM lines to activate this plan.</p>

        <form [formGroup]="subscribeForm" (ngSubmit)="onConfirmSubscription()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Select SIM Line</label>
            <select formControlName="lineId" class="form-control form-select">
              <option value="" disabled>-- Select Active Line --</option>
              <option *ngFor="let sim of subscriberLines" [value]="sim.lineId">
                Line #{{ sim.lineId }} - {{ sim.msisdn }} ({{ sim.serviceType }})
              </option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Renewal Configuration</label>
            <select formControlName="renewalType" class="form-control form-select">
              <option value="AutoRenew">AutoRenew</option>
              <option value="Manual">Manual</option>
            </select>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="subscribeForm.invalid" class="btn btn-primary">Confirm Activation</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Apply Addon Modal -->
    <div *ngIf="showAddonModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Apply Add-On: {{ selectedAddon?.name }}</h2>
        <p class="muted-text mt-8">Apply this add-on to an existing active subscription.</p>

        <form [formGroup]="applyAddonForm" (ngSubmit)="onConfirmAddon()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Select Active Subscription</label>
            <select formControlName="subscriptionId" class="form-control form-select">
              <option value="" disabled>-- Select Subscription --</option>
              <option *ngFor="let sub of displayedSubscriptions" [value]="sub.subscriptionId">
                Sub #{{ sub.subscriptionId }} - Line #{{ sub.lineId }} - {{ getPlanName(sub.planId) }}
              </option>
            </select>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="applyAddonForm.invalid" class="btn btn-primary">Purchase Add-On</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Edit Plan Modal (Admin) -->
    <div *ngIf="showEditPlanModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Edit Telecom Plan: {{ selectedPlan?.name }}</h2>
        <form [formGroup]="editPlanForm" (ngSubmit)="onConfirmPlanUpdate()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Plan Name</label>
            <input type="text" formControlName="name" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Plan Type</label>
            <select formControlName="type" class="form-control form-select">
              <option value="Prepaid">Prepaid</option>
              <option value="Postpaid">Postpaid</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Data (GB)</label>
            <input type="number" formControlName="dataGb" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Voice Minutes (-1 for Unlimited)</label>
            <input type="number" formControlName="voiceMinutes" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">SMS Count (-1 for Unlimited)</label>
            <input type="number" formControlName="smsCount" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Validity (Days)</label>
            <input type="number" formControlName="validityDays" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Price ($)</label>
            <input type="number" formControlName="planPrice" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Status</label>
            <select formControlName="status" class="form-control form-select">
              <option value="ACTIVE">ACTIVE</option>
              <option value="INACTIVE">INACTIVE</option>
            </select>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="editPlanForm.invalid" class="btn btn-primary">Save Changes</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Update Subscription Modal (Renewal / Status change) -->
    <div *ngIf="showUpdateSubscriptionModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Manage Subscription #{{ selectedSub?.subscriptionId }}</h2>
        <form [formGroup]="updateSubForm" (ngSubmit)="onConfirmSubscriptionUpdate()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Renewal Configuration</label>
            <select formControlName="renewalType" class="form-control form-select">
              <option value="AutoRenew">AutoRenew</option>
              <option value="Manual">Manual</option>
            </select>
          </div>

          <div class="form-group" *ngIf="isAdmin">
            <label class="form-label">Subscription Status</label>
            <select formControlName="status" class="form-control form-select">
              <option value="A">Active</option>
              <option value="S">Suspended</option>
              <option value="E">Expired</option>
            </select>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="updateSubForm.invalid" class="btn btn-primary">Update Subscription</button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .plans-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .plans-header h1 {
      font-size: 2.2rem;
      margin-bottom: 6px;
    }
    .plans-header p {
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
    .plans-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 24px;
    }
    .plan-card {
      display: flex;
      flex-direction: column;
      position: relative;
    }
    .inactive-plan {
      opacity: 0.6;
    }
    .plan-badge-wrapper {
      display: flex;
      gap: 8px;
      margin-bottom: 12px;
    }
    .plan-name {
      font-size: 1.4rem;
      margin-bottom: 12px;
    }
    .plan-price {
      display: flex;
      align-items: baseline;
      gap: 4px;
      margin-bottom: 20px;
    }
    .plan-price .currency {
      font-size: 1.2rem;
      font-weight: 700;
      color: var(--accent-primary);
    }
    .plan-price .amount {
      font-size: 2.4rem;
      font-weight: 800;
      color: #ffffff;
      line-height: 1;
    }
    .plan-price .period {
      color: var(--text-secondary);
      font-size: 0.9rem;
    }
    .plan-features {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .feature-item {
      display: flex;
      align-items: center;
      gap: 10px;
      color: var(--text-primary);
    }
    .feature-item .icon {
      font-size: 1.1rem;
    }
    .manage-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 24px;
    }
    @media (max-width: 768px) {
      .manage-grid {
        grid-template-columns: 1fr;
      }
    }
    /* Modal styles */
    .modal-backdrop {
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      height: 100vh;
      background: rgba(0, 0, 0, 0.6);
      backdrop-filter: blur(8px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .modal-content {
      width: 100%;
      max-width: 500px;
      padding: 32px;
    }
    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }
  `]
})
export class PlansAddonsComponent implements OnInit {
  private readonly planService = inject(PlanService);
  private readonly subscriberService = inject(SubscriberService);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  activeTab = 'plans';
  plans: TelecomPlanResponse[] = [];
  addons: AddOnResponse[] = [];
  allSubscriptions: ServiceSubscriptionResponse[] = [];
  displayedSubscriptions: ServiceSubscriptionResponse[] = [];

  // Account / SIM Line info for logged-in Subscriber
  subscriberAccounts: AccountResponse[] = [];
  subscriberLines: SimLineResponse[] = [];

  // Modals state
  showSubscribeModal = false;
  showAddonModal = false;
  showEditPlanModal = false;
  showUpdateSubscriptionModal = false;

  selectedPlan: TelecomPlanResponse | null = null;
  selectedAddon: AddOnResponse | null = null;
  selectedSub: ServiceSubscriptionResponse | null = null;

  // Forms
  planForm!: FormGroup;
  addonForm!: FormGroup;
  subscribeForm!: FormGroup;
  applyAddonForm!: FormGroup;
  editPlanForm!: FormGroup;
  updateSubForm!: FormGroup;

  // Feedback alerts
  alertMessage = '';
  successMessage = '';
  errorMessage = '';

  get isAdmin(): boolean {
    return this.authService.hasPermission('MANAGE_PLANS') || this.authService.userRole() === 'A';
  }

  get isSubscriber(): boolean {
    return this.authService.userRole() === 'S';
  }

  ngOnInit() {
    this.initForms();
    this.loadData();
  }

  setTab(tab: string) {
    this.activeTab = tab;
    this.clearAlerts();
  }

  private initForms() {
    this.planForm = this.fb.group({
      name: ['', Validators.required],
      type: ['Prepaid', Validators.required],
      dataGb: [10, [Validators.required, Validators.min(0)]],
      voiceMinutes: [-1, Validators.required],
      smsCount: [-1, Validators.required],
      validityDays: [30, [Validators.required, Validators.min(1)]],
      planPrice: [19.99, [Validators.required, Validators.min(0)]]
    });

    this.addonForm = this.fb.group({
      name: ['', Validators.required],
      type: ['DataTopup', Validators.required],
      quota: [5, [Validators.required, Validators.min(0)]],
      validityDays: [30, [Validators.required, Validators.min(1)]],
      price: [9.99, [Validators.required, Validators.min(0)]]
    });

    this.subscribeForm = this.fb.group({
      lineId: ['', Validators.required],
      renewalType: ['AutoRenew', Validators.required]
    });

    this.applyAddonForm = this.fb.group({
      subscriptionId: ['', Validators.required]
    });

    this.editPlanForm = this.fb.group({
      name: ['', Validators.required],
      type: ['Prepaid', Validators.required],
      dataGb: [0, Validators.required],
      voiceMinutes: [0, Validators.required],
      smsCount: [0, Validators.required],
      validityDays: [0, Validators.required],
      planPrice: [0, Validators.required],
      status: ['ACTIVE', Validators.required]
    });

    this.updateSubForm = this.fb.group({
      renewalType: ['AutoRenew', Validators.required],
      status: ['A', Validators.required]
    });
  }

  private clearAlerts() {
    this.alertMessage = '';
    this.successMessage = '';
    this.errorMessage = '';
  }

  private loadData() {
    this.clearAlerts();
    // Load plans & addons
    this.planService.getAllPlans().subscribe({
      next: (res) => this.plans = res,
      error: () => this.errorMessage = 'Failed to load plans.'
    });

    this.planService.getAllAddOns().subscribe({
      next: (res) => this.addons = res,
      error: () => this.errorMessage = 'Failed to load add-ons.'
    });

    // Load subscriptions
    if (this.isAdmin) {
      this.planService.getAllSubscriptions().subscribe({
        next: (res) => {
          this.allSubscriptions = res;
          this.displayedSubscriptions = res;
        },
        error: () => this.errorMessage = 'Failed to load user subscriptions.'
      });
    } else if (this.isSubscriber) {
      // Find my own plans and accounts
      this.userService.getMe().subscribe({
        next: (user) => {
          this.subscriberService.getAllAccounts(undefined, user.userId).subscribe({
            next: (accountsRes) => {
              this.subscriberAccounts = accountsRes.subscribers;
              this.subscriberLines = [];
              
              // Load all SIM lines associated with subscriber's accounts
              this.subscriberAccounts.forEach(acc => {
                this.subscriberService.getSimLines(acc.accountId).subscribe({
                  next: (lines) => {
                    // filter to keep only Active SIM lines for subscription
                    this.subscriberLines.push(...lines.filter(l => l.status === 'Active'));
                    
                    // Fetch all subscriptions for these line IDs
                    this.planService.getAllSubscriptions().subscribe({
                      next: (subs) => {
                        this.allSubscriptions = subs;
                        const lineIds = this.subscriberLines.map(l => l.lineId);
                        this.displayedSubscriptions = subs.filter(s => lineIds.includes(s.lineId));
                      }
                    });
                  }
                });
              });
            }
          });
        }
      });
    }
  }

  getPlanName(planId: number | null): string {
    if (!planId) return 'N/A';
    const plan = this.plans.find(p => p.planId === planId);
    return plan ? plan.name : `Plan #${planId}`;
  }

  getAddonName(addOnId: number | null): string {
    if (!addOnId) return '';
    const addon = this.addons.find(a => a.addOnId === addOnId);
    return addon ? addon.name : `AddOn #${addOnId}`;
  }

  // Subscribing to plans
  openSubscribeModal(plan: TelecomPlanResponse) {
    this.selectedPlan = plan;
    this.subscribeForm.reset({
      lineId: '',
      renewalType: 'AutoRenew'
    });
    this.showSubscribeModal = true;
  }

  onConfirmSubscription() {
    if (this.subscribeForm.invalid || !this.selectedPlan) return;
    const { lineId, renewalType } = this.subscribeForm.value;

    const activationDate = new Date();
    const expiryDate = new Date();
    expiryDate.setDate(activationDate.getDate() + this.selectedPlan.validityDays);

    const req = {
      lineId: Number(lineId),
      planId: this.selectedPlan.planId,
      activationDate: activationDate.toISOString().split('T')[0],
      expiryDate: expiryDate.toISOString().split('T')[0],
      renewalType,
      status: 'A'
    };

    this.planService.createSubscription(req).subscribe({
      next: () => {
        this.successMessage = 'Plan subscription created successfully!';
        this.closeModals();
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to create subscription.'
    });
  }

  // Applying add-on to active subscription
  openApplyAddonModal(addon: AddOnResponse) {
    this.selectedAddon = addon;
    this.applyAddonForm.reset({
      subscriptionId: ''
    });
    this.showAddonModal = true;
  }

  onConfirmAddon() {
    if (this.applyAddonForm.invalid || !this.selectedAddon) return;
    const { subscriptionId } = this.applyAddonForm.value;

    const req = {
      addOnId: this.selectedAddon.addOnId
    };

    this.planService.updateSubscription(Number(subscriptionId), req).subscribe({
      next: () => {
        this.successMessage = `Add-on ${this.selectedAddon?.name} applied successfully!`;
        this.closeModals();
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to apply add-on.'
    });
  }

  // Create Telecom Plan
  onCreatePlan() {
    if (this.planForm.invalid) return;
    this.planService.createPlan(this.planForm.value).subscribe({
      next: () => {
        this.successMessage = 'Telecom Plan created successfully!';
        this.planForm.reset({
          type: 'Prepaid',
          dataGb: 10,
          voiceMinutes: -1,
          smsCount: -1,
          validityDays: 30,
          planPrice: 19.99
        });
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to create plan.'
    });
  }

  // Create Add-on
  onCreateAddon() {
    if (this.addonForm.invalid) return;
    this.planService.createAddOn(this.addonForm.value).subscribe({
      next: () => {
        this.successMessage = 'Add-On created successfully!';
        this.addonForm.reset({
          type: 'DataTopup',
          quota: 5,
          validityDays: 30,
          price: 9.99
        });
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to create Add-on.'
    });
  }

  // Edit Plan
  openEditPlanModal(plan: TelecomPlanResponse) {
    this.selectedPlan = plan;
    this.editPlanForm.setValue({
      name: plan.name,
      type: plan.type,
      dataGb: plan.dataGb,
      voiceMinutes: plan.voiceMinutes,
      smsCount: plan.smsCount,
      validityDays: plan.validityDays,
      planPrice: plan.planPrice,
      status: plan.status
    });
    this.showEditPlanModal = true;
  }

  onConfirmPlanUpdate() {
    if (this.editPlanForm.invalid || !this.selectedPlan) return;
    this.planService.updatePlan(this.selectedPlan.planId, this.editPlanForm.value).subscribe({
      next: () => {
        this.successMessage = 'Plan updated successfully!';
        this.closeModals();
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to update plan.'
    });
  }

  // Update subscription details (Renewal / Status)
  openUpdateSubscriptionModal(sub: ServiceSubscriptionResponse) {
    this.selectedSub = sub;
    this.updateSubForm.setValue({
      renewalType: sub.renewalType,
      status: sub.status
    });
    this.showUpdateSubscriptionModal = true;
  }

  onConfirmSubscriptionUpdate() {
    if (this.updateSubForm.invalid || !this.selectedSub) return;
    this.planService.updateSubscription(this.selectedSub.subscriptionId, this.updateSubForm.value).subscribe({
      next: () => {
        this.successMessage = 'Subscription updated successfully!';
        this.closeModals();
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to update subscription.'
    });
  }

  closeModals() {
    this.showSubscribeModal = false;
    this.showAddonModal = false;
    this.showEditPlanModal = false;
    this.showUpdateSubscriptionModal = false;
    this.selectedPlan = null;
    this.selectedAddon = null;
    this.selectedSub = null;
  }
}
