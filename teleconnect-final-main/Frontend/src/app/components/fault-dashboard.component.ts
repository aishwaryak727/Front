import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { FaultService, FaultTicketResponse, ServiceRequestResponse } from '../services/fault.service';
import { SubscriberService, AccountResponse, SimLineResponse } from '../services/subscriber.service';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-fault-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="faults-container animate-fade-in">
      <header class="faults-header">
        <h1>Fault Management & Support</h1>
        <p>Raise and track network faults, device issues, and service requests (SIM swaps, porting)</p>
      </header>

      <!-- Main Tab Selection -->
      <div class="tabs">
        <button [class.active]="activeTab === 'tickets'" (click)="setTab('tickets')" class="tab-btn">
          🎫 Fault Tickets
        </button>
        <button [class.active]="activeTab === 'requests'" (click)="setTab('requests')" class="tab-btn">
          ⚡ Service Requests
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

      <!-- Fault Tickets Tab -->
      <div *ngIf="activeTab === 'tickets'" class="tab-content animate-fade-in">
        <div class="glass-card">
          <div class="panel-header mb-16">
            <h3>Fault Tickets</h3>
            <!-- Raise Ticket Button (Available to users with SERVICE_REQUEST authority) -->
            <button *ngIf="authService.hasPermission('SERVICE_REQUEST')" (click)="openRaiseTicketModal()" class="btn btn-primary btn-small">
              + Raise Fault Ticket
            </button>
          </div>

          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Ticket ID</th>
                  <th>Account ID</th>
                  <th>SIM Line ID</th>
                  <th>Type</th>
                  <th>Priority</th>
                  <th>Description</th>
                  <th>Raised Date</th>
                  <th>Status</th>
                  <th>Assigned Operator</th>
                  <th>Resolved Date</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let t of tickets">
                  <td><code>#{{ t.ticketId }}</code></td>
                  <td><code>#{{ t.accountId }}</code></td>
                  <td><code>#{{ t.lineId }}</code></td>
                  <td>
                    <span class="badge badge-info">{{ t.faultType }}</span>
                  </td>
                  <td>
                    <span class="badge" [ngClass]="t.priority === 'H' ? 'badge-danger' : (t.priority === 'M' ? 'badge-warning' : 'badge-secondary')">
                      {{ t.priority === 'H' ? 'High' : (t.priority === 'M' ? 'Medium' : 'Low') }}
                    </span>
                  </td>
                  <td class="desc-cell" [title]="t.description">{{ t.description }}</td>
                  <td>{{ t.raisedDate }}</td>
                  <td>
                    <span class="badge" [ngClass]="t.status === 'RESOLVED' ? 'badge-success' : (t.status === 'ASSIGNED' ? 'badge-warning' : 'badge-info')">
                      {{ t.status }}
                    </span>
                  </td>
                  <td>
                    <span *ngIf="t.assignedToId">Operator #{{ t.assignedToId }}</span>
                    <span *ngIf="!t.assignedToId" class="muted-text">Unassigned</span>
                  </td>
                  <td>{{ t.resolvedDate || '-' }}</td>
                  <td class="action-cell">
                    <!-- Assign Operator (RESOLVE_TICKET permission required) -->
                    <button *ngIf="authService.hasPermission('RESOLVE_TICKET') && t.status !== 'RESOLVED'" (click)="openAssignModal(t)" class="btn btn-secondary btn-small">
                      Assign
                    </button>
                    <!-- Update Status/Priority (GET_UPDATE_TICKET permission required) -->
                    <button *ngIf="authService.hasPermission('GET_UPDATE_TICKET') && t.status !== 'RESOLVED'" (click)="openUpdateTicketModal(t)" class="btn btn-secondary btn-small">
                      Edit
                    </button>
                    <!-- Resolve Ticket (RESOLVE_TICKET permission required) -->
                    <button *ngIf="authService.hasPermission('RESOLVE_TICKET') && t.status !== 'RESOLVED'" (click)="openResolveModal(t)" class="btn btn-primary btn-small">
                      Resolve
                    </button>
                  </td>
                </tr>
                <tr *ngIf="tickets.length === 0">
                  <td colspan="11" class="text-center muted-text py-20">No fault tickets found.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Service Requests Tab -->
      <div *ngIf="activeTab === 'requests'" class="tab-content animate-fade-in">
        <div class="glass-card">
          <div class="panel-header mb-16">
            <h3>Service Requests</h3>
            <!-- Submit Service Request (Available to users with SERVICE_REQUEST authority) -->
            <button *ngIf="authService.hasPermission('SERVICE_REQUEST')" (click)="openCreateRequestModal()" class="btn btn-primary btn-small">
              + Submit Service Request
            </button>
          </div>

          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Request ID</th>
                  <th>Account ID</th>
                  <th>SIM Line ID</th>
                  <th>Request Type</th>
                  <th>Requested By</th>
                  <th>Raised Date</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let r of requests">
                  <td><code>#{{ r.requestId }}</code></td>
                  <td><code>#{{ r.accountId }}</code></td>
                  <td><code>#{{ r.lineId }}</code></td>
                  <td><strong>{{ r.requestType }}</strong></td>
                  <td>User #{{ r.requestedBy }}</td>
                  <td>{{ r.raisedDate }}</td>
                  <td>
                    <span class="badge" [ngClass]="r.status === 'C' ? 'badge-success' : (r.status === 'X' ? 'badge-danger' : 'badge-info')">
                      {{ r.status === 'O' ? 'Open' : (r.status === 'C' ? 'Completed' : 'Canceled') }}
                    </span>
                  </td>
                  <td class="action-cell">
                    <!-- Update Request Status (Staff only - SERVICE_REQUEST) -->
                    <button *ngIf="!isSubscriber && r.status === 'O'" (click)="openUpdateRequestModal(r)" class="btn btn-secondary btn-small">
                      Update
                    </button>
                    <!-- Cancel Request (Subscriber or Staff - SERVICE_REQUEST) -->
                    <button *ngIf="r.status === 'O'" (click)="cancelRequest(r.requestId)" class="btn btn-danger btn-small">
                      Cancel
                    </button>
                  </td>
                </tr>
                <tr *ngIf="requests.length === 0">
                  <td colspan="8" class="text-center muted-text py-20">No service requests found.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Modals -->

    <!-- Raise Fault Ticket Modal -->
    <div *ngIf="showRaiseTicketModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Raise Fault Ticket</h2>
        <form [formGroup]="ticketForm" (ngSubmit)="onConfirmRaiseTicket()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Select Associated SIM Line</label>
            <select formControlName="lineSelector" (change)="onTicketLineSelected()" class="form-control form-select">
              <option value="" disabled>-- Select SIM Line --</option>
              <option *ngFor="let sim of subscriberLines" [value]="sim.lineId">
                {{ sim.msisdn }} (Account #{{ sim.accountId }}, Line #{{ sim.lineId }})
              </option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Fault Category Type</label>
            <select formControlName="faultType" class="form-control form-select">
              <option value="NETWORK">Network Outage / Coverage</option>
              <option value="DEVICE">Device Troubleshooting</option>
              <option value="BILLING">Billing Discrepancy</option>
              <option value="SIMCARD">Physical SIM Card Damaged</option>
              <option value="OTHER">Other Issues</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Priority</label>
            <select formControlName="priority" class="form-control form-select">
              <option value="L">Low Priority</option>
              <option value="M">Medium Priority</option>
              <option value="H">High Priority</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Issue Details & Description</label>
            <textarea formControlName="description" class="form-control" rows="3" placeholder="Describe the fault symptoms..."></textarea>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="ticketForm.invalid" class="btn btn-primary">Raise Ticket</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Submit Service Request Modal -->
    <div *ngIf="showCreateRequestModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Submit Service Request</h2>
        <form [formGroup]="requestForm" (ngSubmit)="onConfirmCreateRequest()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Select Associated SIM Line</label>
            <select formControlName="lineSelector" (change)="onRequestLineSelected()" class="form-control form-select">
              <option value="" disabled>-- Select SIM Line --</option>
              <option *ngFor="let sim of subscriberLines" [value]="sim.lineId">
                {{ sim.msisdn }} (Account #{{ sim.accountId }}, Line #{{ sim.lineId }})
              </option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Service Request Type</label>
            <select formControlName="requestType" class="form-control form-select">
              <option value="SIMSwap">SIM Card Swap</option>
              <option value="Porting">Msisdn Porting Request</option>
              <option value="Activation">Line Re-activation</option>
              <option value="Cancellation">Line De-activation</option>
              <option value="Other">Other Service Operations</option>
            </select>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="requestForm.invalid" class="btn btn-primary">Submit Request</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Assign Operator Modal -->
    <div *ngIf="showAssignModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Assign Fault Ticket #{{ selectedTicket?.ticketId }}</h2>
        <form [formGroup]="assignForm" (ngSubmit)="onConfirmAssign()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Assigned Operator/Staff ID</label>
            <input type="number" formControlName="assignedToId" class="form-control">
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="assignForm.invalid" class="btn btn-primary">Assign Operator</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Update Ticket Modal (Staff) -->
    <div *ngIf="showUpdateTicketModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Edit Fault Ticket #{{ selectedTicket?.ticketId }}</h2>
        <form [formGroup]="updateTicketForm" (ngSubmit)="onConfirmUpdateTicket()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Fault Category Type</label>
            <select formControlName="faultType" class="form-control form-select">
              <option value="NETWORK">Network Outage / Coverage</option>
              <option value="DEVICE">Device Troubleshooting</option>
              <option value="BILLING">Billing Discrepancy</option>
              <option value="SIMCARD">Physical SIM Card Damaged</option>
              <option value="OTHER">Other Issues</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Priority</label>
            <select formControlName="priority" class="form-control form-select">
              <option value="L">Low Priority</option>
              <option value="M">Medium Priority</option>
              <option value="H">High Priority</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Status</label>
            <select formControlName="status" class="form-control form-select">
              <option value="OPEN">OPEN</option>
              <option value="ASSIGNED">ASSIGNED</option>
            </select>
          </div>

          <div class="form-group">
            <label class="form-label">Issue Details & Description</label>
            <textarea formControlName="description" class="form-control" rows="3"></textarea>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="updateTicketForm.invalid" class="btn btn-primary">Save Changes</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Resolve Ticket Modal -->
    <div *ngIf="showResolveModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Resolve Fault Ticket #{{ selectedTicket?.ticketId }}</h2>
        <p class="description mb-16">Enter resolution diagnostics to finalize and close this ticket.</p>
        <form [formGroup]="resolveForm" (ngSubmit)="onConfirmResolve()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Troubleshooting Diagnostics / Description</label>
            <textarea formControlName="description" class="form-control" rows="3" placeholder="Troubleshooting details..."></textarea>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="resolveForm.invalid" class="btn btn-primary">Resolve Ticket</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Update Request Status Modal -->
    <div *ngIf="showUpdateRequestModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Update Service Request #{{ selectedRequest?.requestId }}</h2>
        <form [formGroup]="updateRequestForm" (ngSubmit)="onConfirmUpdateRequest()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Status</label>
            <select formControlName="status" class="form-control form-select">
              <option value="O">Open</option>
              <option value="C">Completed</option>
              <option value="X">Canceled</option>
            </select>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="updateRequestForm.invalid" class="btn btn-primary">Update Request</button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .faults-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .faults-header h1 {
      font-size: 2.2rem;
      margin-bottom: 6px;
    }
    .faults-header p {
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
    .desc-cell {
      max-width: 250px;
      white-space: nowrap;
      text-overflow: ellipsis;
      overflow: hidden;
    }
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
export class FaultDashboardComponent implements OnInit {
  private readonly faultService = inject(FaultService);
  private readonly subscriberService = inject(SubscriberService);
  private readonly userService = inject(UserService);
  readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  activeTab = 'tickets';
  tickets: FaultTicketResponse[] = [];
  requests: ServiceRequestResponse[] = [];

  // Subscriber associated accounts & SIM lines
  subscriberAccounts: AccountResponse[] = [];
  subscriberLines: SimLineResponse[] = [];
  currentUserId!: number;

  // Modals state
  showRaiseTicketModal = false;
  showCreateRequestModal = false;
  showAssignModal = false;
  showUpdateTicketModal = false;
  showResolveModal = false;
  showUpdateRequestModal = false;

  selectedTicket: FaultTicketResponse | null = null;
  selectedRequest: ServiceRequestResponse | null = null;

  // Forms
  ticketForm!: FormGroup;
  requestForm!: FormGroup;
  assignForm!: FormGroup;
  updateTicketForm!: FormGroup;
  resolveForm!: FormGroup;
  updateRequestForm!: FormGroup;

  // Alerts
  alertMessage = '';
  successMessage = '';
  errorMessage = '';

  get isSubscriber(): boolean {
    return this.authService.userRole() === 'S';
  }

  ngOnInit() {
    this.initForms();
    this.loadData();
    this.loadSubscriberLines();
  }

  setTab(tab: string) {
    this.activeTab = tab;
    this.clearAlerts();
    this.loadData();
  }

  private initForms() {
    this.ticketForm = this.fb.group({
      lineSelector: ['', Validators.required],
      accountId: ['', Validators.required],
      lineId: ['', Validators.required],
      faultType: ['NETWORK', Validators.required],
      priority: ['M', Validators.required],
      description: ['', Validators.required]
    });

    this.requestForm = this.fb.group({
      lineSelector: ['', Validators.required],
      accountId: ['', Validators.required],
      lineId: ['', Validators.required],
      requestType: ['SIMSwap', Validators.required]
    });

    this.assignForm = this.fb.group({
      assignedToId: ['', [Validators.required, Validators.min(1)]]
    });

    this.updateTicketForm = this.fb.group({
      faultType: ['NETWORK', Validators.required],
      priority: ['M', Validators.required],
      status: ['OPEN', Validators.required],
      description: ['', Validators.required]
    });

    this.resolveForm = this.fb.group({
      description: ['', Validators.required]
    });

    this.updateRequestForm = this.fb.group({
      status: ['O', Validators.required]
    });
  }

  private clearAlerts() {
    this.alertMessage = '';
    this.successMessage = '';
    this.errorMessage = '';
  }

  private loadData() {
    this.clearAlerts();
    
    if (this.activeTab === 'tickets') {
      this.faultService.getAllTickets().subscribe({
        next: (res) => {
          if (this.isSubscriber) {
            // Filter to show only tickets associated with subscriber's active line IDs
            const lineIds = this.subscriberLines.map(l => l.lineId);
            this.tickets = res.filter(t => lineIds.includes(t.lineId));
          } else {
            this.tickets = res;
          }
        },
        error: () => this.errorMessage = 'Failed to load fault tickets.'
      });
    }

    if (this.activeTab === 'requests') {
      this.faultService.getAllRequests().subscribe({
        next: (res) => {
          if (this.isSubscriber) {
            this.requests = res.filter(r => r.requestedBy === this.currentUserId);
          } else {
            this.requests = res;
          }
        },
        error: () => this.errorMessage = 'Failed to load service requests.'
      });
    }
  }

  private loadSubscriberLines() {
    this.userService.getMe().subscribe({
      next: (user) => {
        this.currentUserId = user.userId;
        this.subscriberService.getAllAccounts(undefined, user.userId).subscribe({
          next: (accountsRes) => {
            this.subscriberAccounts = accountsRes.subscribers || [];
            this.subscriberLines = [];

            this.subscriberAccounts.forEach(acc => {
              this.subscriberService.getSimLines(acc.accountId).subscribe({
                next: (lines) => {
                  this.subscriberLines.push(...lines.filter(l => l.status === 'Active'));
                  // Reload lists now that lineIds filter lists are resolved
                  this.loadData();
                }
              });
            });
          }
        });
      }
    });
  }

  // Raising tickets
  openRaiseTicketModal() {
    this.ticketForm.reset({
      lineSelector: '',
      faultType: 'NETWORK',
      priority: 'M',
      description: ''
    });
    this.showRaiseTicketModal = true;
  }

  onTicketLineSelected() {
    const selectedLineId = this.ticketForm.value.lineSelector;
    const sim = this.subscriberLines.find(l => l.lineId === Number(selectedLineId));
    if (sim) {
      this.ticketForm.patchValue({
        accountId: sim.accountId,
        lineId: sim.lineId
      });
    }
  }

  onConfirmRaiseTicket() {
    if (this.ticketForm.invalid) return;
    const req = {
      accountId: Number(this.ticketForm.value.accountId),
      lineId: Number(this.ticketForm.value.lineId),
      faultType: this.ticketForm.value.faultType,
      priority: this.ticketForm.value.priority,
      description: this.ticketForm.value.description,
      raisedDate: new Date().toISOString().split('T')[0]
    };

    this.faultService.createTicket(req).subscribe({
      next: () => {
        this.successMessage = 'Fault ticket raised successfully!';
        this.closeModals();
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to raise fault ticket.'
    });
  }

  // Create Service Request
  openCreateRequestModal() {
    this.requestForm.reset({
      lineSelector: '',
      requestType: 'SIMSwap'
    });
    this.showCreateRequestModal = true;
  }

  onRequestLineSelected() {
    const selectedLineId = this.requestForm.value.lineSelector;
    const sim = this.subscriberLines.find(l => l.lineId === Number(selectedLineId));
    if (sim) {
      this.requestForm.patchValue({
        accountId: sim.accountId,
        lineId: sim.lineId
      });
    }
  }

  onConfirmCreateRequest() {
    if (this.requestForm.invalid) return;
    const req = {
      accountId: Number(this.requestForm.value.accountId),
      lineId: Number(this.requestForm.value.lineId),
      requestType: this.requestForm.value.requestType,
      requestedBy: this.currentUserId,
      raisedDate: new Date().toISOString().split('T')[0]
    };

    this.faultService.createRequest(req).subscribe({
      next: () => {
        this.successMessage = 'Service request submitted successfully!';
        this.closeModals();
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to submit service request.'
    });
  }

  // Assign Operator
  openAssignModal(t: FaultTicketResponse) {
    this.selectedTicket = t;
    this.assignForm.reset({ assignedToId: '' });
    this.showAssignModal = true;
  }

  onConfirmAssign() {
    if (this.assignForm.invalid || !this.selectedTicket) return;
    const req = {
      accountId: this.selectedTicket.accountId,
      lineId: this.selectedTicket.lineId,
      faultType: this.selectedTicket.faultType,
      description: this.selectedTicket.description,
      priority: this.selectedTicket.priority,
      raisedDate: this.selectedTicket.raisedDate,
      assignedToId: Number(this.assignForm.value.assignedToId)
    };

    this.faultService.assignTicket(this.selectedTicket.ticketId, req).subscribe({
      next: () => {
        this.successMessage = 'Operator assigned to fault ticket!';
        this.closeModals();
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to assign operator.'
    });
  }

  // Update Ticket Details (Staff)
  openUpdateTicketModal(t: FaultTicketResponse) {
    this.selectedTicket = t;
    this.updateTicketForm.setValue({
      faultType: t.faultType,
      priority: t.priority,
      status: t.status,
      description: t.description
    });
    this.showUpdateTicketModal = true;
  }

  onConfirmUpdateTicket() {
    if (this.updateTicketForm.invalid || !this.selectedTicket) return;
    const req = {
      accountId: this.selectedTicket.accountId,
      lineId: this.selectedTicket.lineId,
      raisedDate: this.selectedTicket.raisedDate,
      assignedToId: this.selectedTicket.assignedToId || undefined,
      ...this.updateTicketForm.value
    };

    this.faultService.updateTicket(this.selectedTicket.ticketId, req).subscribe({
      next: () => {
        this.successMessage = 'Fault ticket updated successfully!';
        this.closeModals();
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to update fault ticket.'
    });
  }

  // Resolve Ticket
  openResolveModal(t: FaultTicketResponse) {
    this.selectedTicket = t;
    this.resolveForm.setValue({
      description: t.description
    });
    this.showResolveModal = true;
  }

  onConfirmResolve() {
    if (this.resolveForm.invalid || !this.selectedTicket) return;
    const req = {
      accountId: this.selectedTicket.accountId,
      lineId: this.selectedTicket.lineId,
      faultType: this.selectedTicket.faultType,
      description: this.resolveForm.value.description,
      priority: this.selectedTicket.priority,
      raisedDate: this.selectedTicket.raisedDate,
      resolvedDate: new Date().toISOString().split('T')[0]
    };

    this.faultService.resolveTicket(this.selectedTicket.ticketId, req).subscribe({
      next: () => {
        this.successMessage = 'Fault ticket resolved successfully!';
        this.closeModals();
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to resolve fault ticket.'
    });
  }

  // Update Service Request Status
  openUpdateRequestModal(r: ServiceRequestResponse) {
    this.selectedRequest = r;
    this.updateRequestForm.setValue({
      status: r.status
    });
    this.showUpdateRequestModal = true;
  }

  onConfirmUpdateRequest() {
    if (this.updateRequestForm.invalid || !this.selectedRequest) return;
    const req = {
      accountId: this.selectedRequest.accountId,
      lineId: this.selectedRequest.lineId,
      requestType: this.selectedRequest.requestType,
      requestedBy: this.selectedRequest.requestedBy,
      raisedDate: this.selectedRequest.raisedDate,
      status: this.updateRequestForm.value.status
    };

    this.faultService.updateRequest(this.selectedRequest.requestId, req).subscribe({
      next: () => {
        this.successMessage = 'Service request updated successfully!';
        this.closeModals();
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to update request.'
    });
  }

  // Cancel Request
  cancelRequest(requestId: number) {
    this.faultService.cancelRequest(requestId).subscribe({
      next: () => {
        this.successMessage = 'Service request canceled successfully!';
        this.loadData();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to cancel service request.'
    });
  }

  closeModals() {
    this.showRaiseTicketModal = false;
    this.showCreateRequestModal = false;
    this.showAssignModal = false;
    this.showUpdateTicketModal = false;
    this.showResolveModal = false;
    this.showUpdateRequestModal = false;
    this.selectedTicket = null;
    this.selectedRequest = null;
  }
}
