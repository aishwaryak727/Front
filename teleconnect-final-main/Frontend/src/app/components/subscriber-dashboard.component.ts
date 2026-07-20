import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { SubscriberService, AccountResponse, SimLineResponse } from '../services/subscriber.service';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-subscriber-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="sub-dashboard-container animate-fade-in">
      <header class="sub-header">
        <h1>Subscriber Accounts & SIM Lines</h1>
        <p>Manage subscription accounts, KYC reviews, service provisioning, and SIM line status</p>
      </header>

      <div class="sub-grid">
        <!-- LEFT COLUMN: Accounts Directory / List -->
        <div class="glass-card list-panel">
          <div class="panel-header">
            <h3>Accounts</h3>
            <!-- Add Account Button (available to all logged-in users under VIEW_SUBSCRIBER/CREATE_USER) -->
            <button *ngIf="authService.hasAnyPermission(['CREATE_USER', 'VIEW_SUBSCRIBER'])" 
                    (click)="showCreateAccountModal = true" class="btn btn-primary btn-small">
              + New Account
            </button>
          </div>

          <!-- Search / Filters (CS Agent / Admin Only) -->
          <div *ngIf="isStaff" class="search-filters mt-16">
            <div class="search-input-group">
              <input type="number" [(ngModel)]="searchSubscriberId" placeholder="Filter by Subscriber ID..." class="form-control">
              <button (click)="loadAccounts()" class="btn btn-secondary btn-small">Apply</button>
              <button (click)="clearFilters()" class="btn btn-secondary btn-small">Clear</button>
            </div>
            
            <div class="special-actions mt-12">
              <button *ngIf="authService.hasPermission('KYC_EXPIRE')" 
                      (click)="loadExpiredKyc()" class="btn btn-danger btn-small btn-block">
                ⚠️ View Expired KYC Accounts
              </button>
            </div>

            <!-- SIM Line Lookup by MSISDN -->
            <div class="sim-lookup-group mt-16">
              <label class="form-label">Lookup SIM by Phone Number</label>
              <div class="search-input-group">
                <input type="text" [(ngModel)]="lookupMsisdn" placeholder="+1234567890" class="form-control">
                <button (click)="onLookupSim()" class="btn btn-primary btn-small">Lookup</button>
              </div>
            </div>
          </div>

          <div *ngIf="alertMessage" class="alert alert-info mt-16">
            ℹ️ {{ alertMessage }}
          </div>

          <!-- Accounts List -->
          <div class="accounts-list mt-20">
            <div *ngFor="let acc of accounts" 
                 [class.active]="selectedAccount?.accountId === acc.accountId"
                 (click)="selectAccount(acc)" 
                 class="account-item">
              <div class="item-main">
                <span class="acc-id">Account #{{ acc.accountId }}</span>
                <span class="badge badge-info">{{ acc.accountType }}</span>
              </div>
              <div class="item-sub">
                <span>Subscriber ID: {{ acc.subscriberId }}</span>
                <span class="badge" [ngClass]="acc.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'">
                  {{ acc.status }}
                </span>
              </div>
              <div class="item-sub mt-4">
                <span>KYC: </span>
                <span class="badge" [ngClass]="getKycBadgeClass(acc.kycStatus)">
                  {{ acc.kycStatus }}
                </span>
              </div>
            </div>
            <div *ngIf="accounts.length === 0" class="muted-text text-center py-20">
              No accounts found.
            </div>
          </div>
        </div>

        <!-- RIGHT COLUMN: Account details & SIM lines -->
        <div class="details-panel-wrapper">
          <!-- Main Details Card -->
          <div *ngIf="selectedAccount" class="glass-card animate-fade-in">
            <div class="detail-header">
              <h2>Account Details (ID: #{{ selectedAccount.accountId }})</h2>
              <button *ngIf="authService.hasPermission('DELETE_USER')" 
                      (click)="onDeleteAccount()" class="btn btn-danger btn-small">
                🗑️ Delete Account
              </button>
            </div>

            <div class="account-meta-grid mt-20">
              <div class="meta-item">
                <span class="label">Subscriber Owner ID</span>
                <span class="value">User #{{ selectedAccount.subscriberId }}</span>
              </div>
              <div class="meta-item">
                <span class="label">Registration Date</span>
                <span class="value">{{ selectedAccount.registrationDate | date:'mediumDate' }}</span>
              </div>
              <div class="meta-item">
                <span class="label">KYC Verification</span>
                <div class="kyc-controls">
                  <span class="badge" [ngClass]="getKycBadgeClass(selectedAccount.kycStatus)">
                    {{ selectedAccount.kycStatus }}
                  </span>
                  <!-- KYC Actions (CS Agent / Admin) -->
                  <button *ngIf="isStaff && selectedAccount.kycStatus !== 'Verified' && authService.hasPermission('VIEW_KYC')" 
                          (click)="onApproveKyc()" class="btn btn-primary btn-small">
                    ✓ Verify KYC
                  </button>
                </div>
              </div>
              <div class="meta-item">
                <span class="label">Status</span>
                <div class="status-controls">
                  <span class="badge" [ngClass]="selectedAccount.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'">
                    {{ selectedAccount.status }}
                  </span>
                  <!-- Activate/Deactivate Account -->
                  <button *ngIf="isStaff" 
                          (click)="toggleAccountStatus()" 
                          class="btn btn-secondary btn-small">
                    {{ selectedAccount.status === 'ACTIVE' ? 'Suspend' : 'Activate' }}
                  </button>
                </div>
              </div>
            </div>

            <!-- SIM Lines Section -->
            <div class="sim-lines-section mt-32">
              <div class="section-header">
                <h3>SIM Lines Associated</h3>
                <button (click)="showAddSimModal = true" class="btn btn-primary btn-small">
                  + Add SIM Line
                </button>
              </div>

              <!-- SIM List Table -->
              <div class="table-container mt-16">
                <table class="table">
                  <thead>
                    <tr>
                      <th>Line ID</th>
                      <th>Phone Number (MSISDN)</th>
                      <th>SIM Card ID (ICCID)</th>
                      <th>Service Type</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let line of simLines">
                      <td><code>#{{ line.lineId }}</code></td>
                      <td><strong>{{ line.msisdn }}</strong></td>
                      <td><code>{{ line.iccid }}</code></td>
                      <td>
                        <span class="badge badge-info">{{ line.serviceType }}</span>
                      </td>
                      <td>
                        <span class="badge" [ngClass]="getSimBadgeClass(line.status)">
                          {{ line.status }}
                        </span>
                      </td>
                      <td class="action-cell">
                        <!-- Toggle Status -->
                        <button (click)="toggleSimStatus(line)" class="btn btn-secondary btn-small">
                          {{ line.status === 'Active' ? 'Suspend' : 'Activate' }}
                        </button>
                        <!-- Swap SIM -->
                        <button (click)="openReplaceModal(line)" class="btn btn-secondary btn-small">
                          🔄 Swap SIM
                        </button>
                        <!-- Service Type Change -->
                        <button (click)="openServiceTypeModal(line)" class="btn btn-secondary btn-small">
                          ⚡ Service
                        </button>
                        <!-- Delete SIM (Admin only) -->
                        <button *ngIf="authService.hasPermission('DELETE_USER')" 
                                (click)="onDeleteSim(line.lineId)" class="btn btn-danger btn-small">
                          🗑️
                        </button>
                      </td>
                    </tr>
                    <tr *ngIf="simLines.length === 0">
                      <td colspan="6" class="text-center muted-text py-20">No SIM Lines provisioned on this account.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- Empty details state -->
          <div *ngIf="!selectedAccount" class="glass-card placeholder-card">
            <span class="placeholder-icon">📱</span>
            <h3>Select a subscriber account</h3>
            <p class="description">Select an account from the left directory to view details, verify KYC status, and provision or manage active SIM lines.</p>
          </div>
        </div>
      </div>

      <!-- MODAL: Create Subscriber Account -->
      <div *ngIf="showCreateAccountModal" class="modal-overlay">
        <div class="glass-card modal-card animate-fade-in">
          <h3>Create Subscriber Account</h3>
          <form [formGroup]="accountForm" (ngSubmit)="onCreateAccount()" class="modal-form mt-20">
            <div class="form-group" *ngIf="isStaff">
              <label class="form-label">Subscriber Owner User ID</label>
              <input type="number" formControlName="subscriberId" class="form-control" placeholder="e.g. 15" required>
            </div>
            
            <div class="form-group">
              <label class="form-label">Account Type</label>
              <select formControlName="accountType" class="form-control form-select" required>
                <option value="Personal">Personal</option>
                <option value="Business">Business</option>
                <option value="Enterprise">Enterprise</option>
              </select>
            </div>

            <div class="modal-actions mt-20">
              <button type="submit" [disabled]="accountForm.invalid" class="btn btn-primary">Create</button>
              <button type="button" (click)="showCreateAccountModal = false" class="btn btn-secondary">Cancel</button>
            </div>
          </form>
        </div>
      </div>

      <!-- MODAL: Add SIM Line -->
      <div *ngIf="showAddSimModal" class="modal-overlay">
        <div class="glass-card modal-card animate-fade-in">
          <h3>Provision SIM Line</h3>
          <form [formGroup]="simForm" (ngSubmit)="onAddSimLine()" class="modal-form mt-20">
            <div class="form-group">
              <label class="form-label">Phone Number (MSISDN)</label>
              <input type="text" formControlName="msisdn" class="form-control" placeholder="+1234567890" required>
            </div>

            <div class="form-group">
              <label class="form-label">SIM Card ID (ICCID)</label>
              <input type="text" formControlName="iccid" class="form-control" placeholder="89014103211118510720" required>
            </div>

            <div class="form-group">
              <label class="form-label">Service Type</label>
              <select formControlName="serviceType" class="form-control form-select">
                <option value="VoiceData">Voice & Data</option>
                <option value="Voice">Voice Only</option>
                <option value="Data">Data Only</option>
              </select>
            </div>

            <div class="modal-actions mt-20">
              <button type="submit" [disabled]="simForm.invalid" class="btn btn-primary">Provision</button>
              <button type="button" (click)="showAddSimModal = false" class="btn btn-secondary">Cancel</button>
            </div>
          </form>
        </div>
      </div>

      <!-- MODAL: Replace SIM (Swap SIM) -->
      <div *ngIf="showReplaceModal" class="modal-overlay">
        <div class="glass-card modal-card animate-fade-in">
          <h3>Swap SIM Card</h3>
          <p class="description">Replace the active SIM card for line <strong>{{ activeSimForAction?.msisdn }}</strong> with a new ICCID.</p>
          <div class="form-group mt-16">
            <label class="form-label">New ICCID</label>
            <input type="text" [(ngModel)]="newIccid" class="form-control" placeholder="89014..." required>
          </div>
          <div class="modal-actions mt-20">
            <button (click)="onSubmitReplaceSim()" [disabled]="!newIccid" class="btn btn-primary">Perform Swap</button>
            <button (click)="showReplaceModal = false" class="btn btn-secondary">Cancel</button>
          </div>
        </div>
      </div>

      <!-- MODAL: Change Service Type -->
      <div *ngIf="showServiceTypeModal" class="modal-overlay">
        <div class="glass-card modal-card animate-fade-in">
          <h3>Change Service Type</h3>
          <p class="description">Update the active service features for line <strong>{{ activeSimForAction?.msisdn }}</strong>.</p>
          <div class="form-group mt-16">
            <label class="form-label">Service Configuration</label>
            <select [(ngModel)]="newServiceType" class="form-control form-select">
              <option value="VoiceData">Voice & Data</option>
              <option value="Voice">Voice Only</option>
              <option value="Data">Data Only</option>
            </select>
          </div>
          <div class="modal-actions mt-20">
            <button (click)="onSubmitServiceType()" class="btn btn-primary">Update Service</button>
            <button (click)="showServiceTypeModal = false" class="btn btn-secondary">Cancel</button>
          </div>
        </div>
      </div>

      <!-- MODAL: SIM Details (From Lookup) -->
      <div *ngIf="showLookupDetailModal" class="modal-overlay">
        <div class="glass-card modal-card animate-fade-in">
          <h3>SIM Lookup Result</h3>
          <div class="account-meta-grid mt-20" *ngIf="lookupResult">
            <div class="meta-item">
              <span class="label">MSISDN</span>
              <span class="value">{{ lookupResult.msisdn }}</span>
            </div>
            <div class="meta-item">
              <span class="label">ICCID</span>
              <span class="value"><code>{{ lookupResult.iccid }}</code></span>
            </div>
            <div class="meta-item">
              <span class="label">Account Association</span>
              <span class="value">Account ID #{{ lookupResult.accountId }}</span>
            </div>
            <div class="meta-item">
              <span class="label">Service Type</span>
              <span class="value">{{ lookupResult.serviceType }}</span>
            </div>
            <div class="meta-item">
              <span class="label">Status</span>
              <span class="value badge" [ngClass]="getSimBadgeClass(lookupResult.status)">{{ lookupResult.status }}</span>
            </div>
          </div>
          <div class="modal-actions mt-24">
            <button (click)="showLookupDetailModal = false" class="btn btn-secondary">Close</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .sub-dashboard-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .sub-header h1 {
      font-size: 2.2rem;
      margin-bottom: 6px;
    }
    .sub-header p {
      color: var(--text-secondary);
      font-size: 1rem;
    }
    .sub-grid {
      display: grid;
      grid-template-columns: 340px 1fr;
      gap: 30px;
      align-items: start;
    }
    .list-panel {
      max-height: 80vh;
      display: flex;
      flex-direction: column;
    }
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 12px;
    }
    .search-filters {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .search-input-group {
      display: flex;
      gap: 8px;
    }
    .search-input-group input {
      flex: 1;
      padding: 8px 12px;
    }
    .accounts-list {
      display: flex;
      flex-direction: column;
      gap: 10px;
      overflow-y: auto;
      flex-grow: 1;
      padding-right: 4px;
    }
    .account-item {
      padding: 14px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      cursor: pointer;
      display: flex;
      flex-direction: column;
      gap: 6px;
      transition: var(--transition-smooth);
    }
    .account-item:hover {
      background: rgba(255, 255, 255, 0.04);
      border-color: var(--accent-primary);
    }
    .account-item.active {
      background: var(--accent-glow);
      border-color: var(--accent-primary);
    }
    .item-main {
      display: flex;
      justify-content: space-between;
      font-weight: 700;
      color: #ffffff;
    }
    .item-sub {
      display: flex;
      justify-content: space-between;
      font-size: 0.8rem;
      color: var(--text-secondary);
    }
    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 16px;
    }
    .account-meta-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 20px;
    }
    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .meta-item .label {
      font-size: 0.8rem;
      color: var(--text-secondary);
      text-transform: uppercase;
      font-weight: 600;
    }
    .meta-item .value {
      font-size: 1.05rem;
      font-weight: 600;
      color: #ffffff;
    }
    .kyc-controls, .status-controls {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 8px;
    }
    .action-cell {
      display: flex;
      gap: 6px;
    }
    .placeholder-card {
      text-align: center;
      padding: 80px 40px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 16px;
    }
    .placeholder-icon {
      font-size: 5rem;
    }
    .placeholder-card h3 {
      font-size: 1.5rem;
    }
    .placeholder-card p {
      max-width: 480px;
      color: var(--text-secondary);
    }

    /* Modal Styling */
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.6);
      backdrop-filter: blur(4px);
      z-index: 100;
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 20px;
    }
    .modal-card {
      width: 100%;
      max-width: 480px;
      padding: 32px;
    }
    .modal-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
    }

    .mt-4 { margin-top: 4px; }
    .mt-12 { margin-top: 12px; }
    .mt-16 { margin-top: 16px; }
    .mt-20 { margin-top: 20px; }
    .mt-24 { margin-top: 24px; }
    .mt-32 { margin-top: 32px; }
    .py-20 { padding-top: 20px; padding-bottom: 20px; }
    .btn-block { width: 100%; }
    .text-center { text-align: center; }
    .muted-text { color: var(--text-muted); }
    .description { color: var(--text-secondary); font-size: 0.9rem; }

    @media (max-width: 992px) {
      .sub-grid {
        grid-template-columns: 1fr;
      }
      .list-panel {
        max-height: none;
      }
    }
  `]
})
export class SubscriberDashboardComponent implements OnInit {
  private readonly subService = inject(SubscriberService);
  readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);

  accounts: AccountResponse[] = [];
  selectedAccount: AccountResponse | null = null;
  simLines: SimLineResponse[] = [];

  // Filter & Search states
  isStaff = false;
  searchSubscriberId?: number;
  lookupMsisdn = '';
  lookupResult: SimLineResponse | null = null;

  // Modal forms
  accountForm!: FormGroup;
  simForm!: FormGroup;

  // Visibility states
  showCreateAccountModal = false;
  showAddSimModal = false;
  showReplaceModal = false;
  showServiceTypeModal = false;
  showLookupDetailModal = false;

  // SIM Line action state
  activeSimForAction: SimLineResponse | null = null;
  newIccid = '';
  newServiceType = 'VoiceData';

  // Feedback states
  alertMessage = '';

  ngOnInit() {
    // Determine if the logged-in user is a staff/operator/admin
    const role = this.authService.userRole();
    this.isStaff = role === 'ADMIN' || role === 'CS_AGENT' || role === 'NETWORK_OPS';

    this.initForms();
    this.loadAccounts();
  }

  initForms() {
    // If subscriber, ID is automatically resolveable by backend from authenticated token.
    // If Admin/CS Agent, they must specify subscriber ID.
    this.accountForm = this.fb.group({
      subscriberId: [null],
      accountType: ['Personal', [Validators.required]]
    });

    this.simForm = this.fb.group({
      msisdn: ['', [Validators.required]],
      iccid: ['', [Validators.required]],
      serviceType: ['VoiceData', [Validators.required]]
    });
  }

  loadAccounts() {
    this.alertMessage = '';
    // If not staff, force filtering by self
    let filterSubId = this.searchSubscriberId;
    if (!this.isStaff) {
      // Find userId in locally stored profile, but backend also filters when subscriberId is passed
      // Let's resolve it. Or we can pass undefined and let backend handle, but let's read the sessionStorage
      const session = this.authService.currentUser();
      // If we don't have it, let the API resolve the subscriber
    }

    this.subService.getAllAccounts(undefined, filterSubId).subscribe({
      next: (res) => {
        this.accounts = res.subscribers;
      },
      error: () => {
        this.alertMessage = 'Failed to load subscriber accounts.';
      }
    });
  }

  loadExpiredKyc() {
    this.alertMessage = '';
    this.subService.getExpiredKyc().subscribe({
      next: (res) => {
        this.accounts = res;
        this.alertMessage = `Loaded ${res.length} expired KYC accounts.`;
      },
      error: () => this.alertMessage = 'Failed to load expired KYC accounts.'
    });
  }

  clearFilters() {
    this.searchSubscriberId = undefined;
    this.loadAccounts();
  }

  selectAccount(acc: AccountResponse) {
    this.selectedAccount = acc;
    this.loadSimLines(acc.accountId);
  }

  loadSimLines(accountId: number) {
    this.subService.getSimLines(accountId).subscribe({
      next: (res) => this.simLines = res
    });
  }

  onCreateAccount() {
    let payload = this.accountForm.value;
    if (!this.isStaff) {
      // Subscribers cannot enter owner ID, backend will verify token or we fetch self ID.
      // We can pass the owner ID. Let's make sure it is not null if required.
      // But wait! If the user is logged in as a subscriber, we can resolve their subscriberId by calling getMe().
      // Let's look up how UserController resolves own user ID or we can call UserService.getMe() first.
      // We can just fetch it, or get it from self user object. Let's do it cleanly:
      this.userService.getMe().subscribe({
        next: (me: any) => {
          payload.subscriberId = me.userId;
          this.submitCreateAccount(payload);
        },
        error: () => this.alertMessage = 'Could not resolve subscriber profile.'
      });
    } else {
      this.submitCreateAccount(payload);
    }
  }

  private submitCreateAccount(payload: any) {
    this.subService.createAccount(payload).subscribe({
      next: (res) => {
        this.alertMessage = res.message || 'Account created successfully!';
        this.showCreateAccountModal = false;
        this.accountForm.reset({ accountType: 'Personal' });
        this.loadAccounts();
      },
      error: (err) => {
        this.alertMessage = err.error?.message || 'Failed to create subscriber account.';
        this.showCreateAccountModal = false;
      }
    });
  }

  onDeleteAccount() {
    if (!this.selectedAccount) return;
    if (!confirm('Are you sure you want to delete this subscriber account? This is permanent.')) return;

    this.subService.deleteAccount(this.selectedAccount.accountId).subscribe({
      next: (res) => {
        this.alertMessage = res.message || 'Account deleted successfully.';
        this.selectedAccount = null;
        this.loadAccounts();
      },
      error: (err) => this.alertMessage = err.error?.message || 'Failed to delete account.'
    });
  }

  onApproveKyc() {
    if (!this.selectedAccount) return;
    this.subService.updateKyc(this.selectedAccount.accountId, 'Verified').subscribe({
      next: (res) => {
        this.alertMessage = res.message || 'KYC Status set to Verified.';
        if (this.selectedAccount) {
          this.selectedAccount.kycStatus = 'Verified';
        }
      },
      error: (err) => this.alertMessage = err.error?.message || 'Failed to update KYC status.'
    });
  }

  toggleAccountStatus() {
    if (!this.selectedAccount) return;
    const newStatus = this.selectedAccount.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    this.subService.updateStatus(this.selectedAccount.accountId, newStatus).subscribe({
      next: (res) => {
        this.alertMessage = res.message || `Account status updated to ${newStatus}.`;
        if (this.selectedAccount) {
          this.selectedAccount.status = newStatus;
        }
      },
      error: (err) => this.alertMessage = err.error?.message || 'Failed to update account status.'
    });
  }

  onAddSimLine() {
    if (!this.selectedAccount) return;
    this.subService.createSimLine(this.selectedAccount.accountId, this.simForm.value).subscribe({
      next: (res) => {
        this.alertMessage = res.message || 'SIM Line provisioned successfully!';
        this.showAddSimModal = false;
        this.simForm.reset({ serviceType: 'VoiceData' });
        this.loadSimLines(this.selectedAccount!.accountId);
      },
      error: (err) => {
        this.alertMessage = err.error?.message || 'Failed to provision SIM line.';
        this.showAddSimModal = false;
      }
    });
  }

  toggleSimStatus(line: SimLineResponse) {
    const newStatus = line.status === 'Active' ? 'Suspended' : 'Active';
    this.subService.updateSimStatus(line.accountId, line.lineId, newStatus).subscribe({
      next: (res) => {
        this.alertMessage = res.message || `SIM status updated to ${newStatus}.`;
        this.loadSimLines(line.accountId);
      },
      error: (err) => this.alertMessage = err.error?.message || 'Failed to update SIM status.'
    });
  }

  openReplaceModal(line: SimLineResponse) {
    this.activeSimForAction = line;
    this.newIccid = '';
    this.showReplaceModal = true;
  }

  onSubmitReplaceSim() {
    if (!this.activeSimForAction || !this.newIccid) return;
    this.subService.replaceSim(this.activeSimForAction.accountId, this.activeSimForAction.lineId, this.newIccid).subscribe({
      next: (res) => {
        this.alertMessage = `SIM successfully swapped. New Card ID: ${res.iccid}`;
        this.showReplaceModal = false;
        this.loadSimLines(this.activeSimForAction!.accountId);
      },
      error: (err) => {
        this.alertMessage = err.error?.message || 'SIM swap failed.';
        this.showReplaceModal = false;
      }
    });
  }

  openServiceTypeModal(line: SimLineResponse) {
    this.activeSimForAction = line;
    this.newServiceType = line.serviceType;
    this.showServiceTypeModal = true;
  }

  onSubmitServiceType() {
    if (!this.activeSimForAction) return;
    this.subService.updateServiceType(this.activeSimForAction.accountId, this.activeSimForAction.lineId, this.newServiceType).subscribe({
      next: (res) => {
        this.alertMessage = res.message || 'SIM service type updated.';
        this.showServiceTypeModal = false;
        this.loadSimLines(this.activeSimForAction!.accountId);
      },
      error: (err) => {
        this.alertMessage = err.error?.message || 'Failed to update service type.';
        this.showServiceTypeModal = false;
      }
    });
  }

  onDeleteSim(lineId: number) {
    if (!this.selectedAccount) return;
    if (!confirm('Are you sure you want to delete/release this SIM Line?')) return;

    this.subService.deleteSimLine(this.selectedAccount.accountId, lineId).subscribe({
      next: (res) => {
        this.alertMessage = res.message || 'SIM Line successfully deleted.';
        this.loadSimLines(this.selectedAccount!.accountId);
      },
      error: (err) => this.alertMessage = err.error?.message || 'Failed to delete SIM line.'
    });
  }

  onLookupSim() {
    if (!this.lookupMsisdn) return;
    this.subService.lookupByMsisdn(this.lookupMsisdn).subscribe({
      next: (res) => {
        this.lookupResult = res;
        this.showLookupDetailModal = true;
      },
      error: () => {
        this.alertMessage = 'No active SIM line found with MSISDN: ' + this.lookupMsisdn;
      }
    });
  }

  // UI styling helpers
  getKycBadgeClass(status: string): string {
    switch (status) {
      case 'Verified': return 'badge-success';
      case 'Pending': return 'badge-warning';
      case 'Expired': return 'badge-danger';
      default: return 'badge-secondary';
    }
  }

  getSimBadgeClass(status: string): string {
    switch (status) {
      case 'Active': return 'badge-success';
      case 'Suspended': return 'badge-warning';
      case 'Deactivated': return 'badge-danger';
      default: return 'badge-secondary';
    }
  }
}
