import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { BillingService, BillingCycleResponse, InvoiceResponse, DisputeResponse, OverdueReportResponse, CollectionReportResponse, DisputeSummaryResponse } from '../services/billing.service';
import { SubscriberService, AccountResponse } from '../services/subscriber.service';
import { UserService } from '../services/user.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-billing-dashboard',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  template: `
    <div class="billing-container animate-fade-in">
      <header class="billing-header">
        <h1>Billing & Invoices</h1>
        <p>Manage subscription accounts invoices, payments, dispute resolutions, and cycles</p>
      </header>

      <!-- Selector & Header Metadata -->
      <div class="glass-card header-controls mb-24">
        <div class="controls-grid">
          <!-- Account Selector (Subscriber) -->
          <div class="form-group mb-0" *ngIf="isSubscriber">
            <label class="form-label">Select Account</label>
            <select [(ngModel)]="selectedAccountId" (change)="onAccountChange()" class="form-control form-select">
              <option value="" disabled>-- Choose Account --</option>
              <option *ngFor="let acc of subscriberAccounts" [value]="acc.accountId">
                Account #{{ acc.accountId }} ({{ acc.accountType }})
              </option>
            </select>
          </div>

          <!-- Account Lookup (CS / Admin) -->
          <div class="form-group mb-0" *ngIf="!isSubscriber">
            <label class="form-label">Look Up Account ID</label>
            <div class="lookup-input-group">
              <input type="number" [(ngModel)]="selectedAccountId" placeholder="e.g. 1" class="form-control">
              <button (click)="onAccountChange()" class="btn btn-primary">Load</button>
            </div>
          </div>

          <!-- Quick Actions -->
          <div class="quick-actions" *ngIf="selectedAccountId">
            <button (click)="downloadStatement(selectedAccountId)" class="btn btn-secondary">
              📥 Download Full Statement
            </button>
            <button *ngIf="isStaff" (click)="triggerMarkOverdue()" class="btn btn-secondary">
              🔄 Mark Overdue Batch
            </button>
          </div>
        </div>
      </div>

      <!-- Main Tabs Navigation -->
      <div class="tabs" *ngIf="selectedAccountId">
        <button [class.active]="activeTab === 'invoices'" (click)="setTab('invoices')" class="tab-btn">
          📄 Invoices
        </button>
        <button [class.active]="activeTab === 'disputes'" (click)="setTab('disputes')" class="tab-btn">
          ⚖️ Disputes
        </button>
        <button *ngIf="isStaff" [class.active]="activeTab === 'cycles'" (click)="setTab('cycles')" class="tab-btn">
          🔄 Billing Cycles
        </button>
        <button *ngIf="hasReportPermission" [class.active]="activeTab === 'reports'" (click)="setTab('reports')" class="tab-btn">
          📊 Reports & Analytics
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

      <!-- Invoices Tab -->
      <div *ngIf="activeTab === 'invoices' && selectedAccountId" class="tab-content animate-fade-in">
        <div class="glass-card">
          <div class="panel-header mb-16">
            <h3>Invoice Records</h3>
            <!-- Generate single invoice button for staff -->
            <button *ngIf="isStaff" (click)="openSingleGenerateModal()" class="btn btn-primary btn-small">
              + Generate Single Invoice
            </button>
          </div>

          <div class="table-container">
            <table class="table">
              <thead>
                <tr>
                  <th>Invoice ID</th>
                  <th>Cycle ID</th>
                  <th>Total Amount</th>
                  <th>Paid</th>
                  <th>Balance Due</th>
                  <th>Due Date</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let inv of invoices">
                  <td><code>#{{ inv.invoiceId }}</code></td>
                  <td><code>#{{ inv.billingCycleId }}</code></td>
                  <td><strong>\${{ inv.totalAmount | number:'1.2-2' }}</strong></td>
                  <td>\${{ inv.amountPaid | number:'1.2-2' }}</td>
                  <td>\${{ (inv.totalAmount - inv.amountPaid) | number:'1.2-2' }}</td>
                  <td>{{ inv.dueDate }}</td>
                  <td>
                    <span class="badge" [ngClass]="getInvoiceBadgeClass(inv.status)">
                      {{ inv.status }}
                    </span>
                  </td>
                  <td class="action-cell flex-wrap">
                    <!-- Download PDF -->
                    <button (click)="downloadInvoice(inv.invoiceId)" class="btn btn-secondary btn-small" title="Download PDF">
                      📄 PDF
                    </button>
                    <!-- Pay invoice -->
                    <button *ngIf="inv.status !== 'PAID'" (click)="openPayModal(inv)" class="btn btn-primary btn-small">
                      Pay
                    </button>
                    <!-- Raise dispute -->
                    <button *ngIf="isSubscriber && inv.status !== 'PAID'" (click)="openRaiseDisputeModal(inv)" class="btn btn-secondary btn-small">
                      Dispute
                    </button>
                    <!-- Send Invoice (Staff only) -->
                    <button *ngIf="isStaff && inv.status === 'DRAFT'" (click)="sendInvoice(inv.invoiceId)" class="btn btn-secondary btn-small">
                      Send
                    </button>
                    <!-- Late fee actions (Staff only) -->
                    <button *ngIf="isStaff && inv.status === 'OVERDUE'" (click)="openLateFeeModal(inv)" class="btn btn-secondary btn-small">
                      + Fee
                    </button>
                    <!-- Waive late fee -->
                    <button *ngIf="isStaff && inv.lateFee > 0 && inv.status !== 'PAID'" (click)="openWaiveFeeModal(inv)" class="btn btn-secondary btn-small">
                      Waive
                    </button>
                  </td>
                </tr>
                <tr *ngIf="invoices.length === 0">
                  <td colspan="8" class="text-center muted-text py-20">No invoice records found.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Disputes Tab -->
      <div *ngIf="activeTab === 'disputes' && selectedAccountId" class="tab-content animate-fade-in">
        <div class="glass-card">
          <h3>Billing Disputes</h3>
          <div class="table-container mt-16">
            <table class="table">
              <thead>
                <tr>
                  <th>Dispute ID</th>
                  <th>Invoice ID</th>
                  <th>Reason</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Assigned To</th>
                  <th>Resolution Details</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let disp of disputes">
                  <td><code>#{{ disp.disputeId }}</code></td>
                  <td><code>#{{ disp.invoiceId }}</code></td>
                  <td><strong class="text-danger">{{ disp.disputeReason }}</strong></td>
                  <td>\${{ disp.disputedAmount | number:'1.2-2' }}</td>
                  <td>
                    <span class="badge" [ngClass]="getDisputeBadgeClass(disp.status)">
                      {{ disp.status }}
                    </span>
                  </td>
                  <td>{{ disp.assignedTo || '-' }}</td>
                  <td>
                    <div *ngIf="disp.resolution">
                      <strong>{{ disp.resolution }}</strong>
                      <div class="text-muted text-small">Credit: \${{ disp.creditAmount }}</div>
                    </div>
                    <span *ngIf="!disp.resolution">-</span>
                  </td>
                  <td class="action-cell">
                    <!-- Under Review (Staff only) -->
                    <button *ngIf="isStaff && disp.status === 'OPEN'" (click)="openReviewDisputeModal(disp)" class="btn btn-secondary btn-small">
                      Review
                    </button>
                    <!-- Resolve (Staff only) -->
                    <button *ngIf="isStaff && (disp.status === 'OPEN' || disp.status === 'UNDER_REVIEW')" (click)="openResolveDisputeModal(disp)" class="btn btn-primary btn-small">
                      Resolve
                    </button>
                  </td>
                </tr>
                <tr *ngIf="disputes.length === 0">
                  <td colspan="8" class="text-center muted-text py-20">No billing disputes found.</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Billing Cycles Tab (Staff Only) -->
      <div *ngIf="activeTab === 'cycles' && isStaff" class="tab-content animate-fade-in">
        <div class="cycles-layout">
          <!-- Cycles list -->
          <div class="glass-card list-panel">
            <div class="panel-header mb-16">
              <h3>Cycle Records</h3>
              <button (click)="showCreateCycleModal = true" class="btn btn-primary btn-small">
                + New Cycle
              </button>
            </div>

            <div class="table-container">
              <table class="table">
                <thead>
                  <tr>
                    <th>Cycle ID</th>
                    <th>Start Date</th>
                    <th>End Date</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let cyc of cycles">
                    <td><code>#{{ cyc.cycleId }}</code></td>
                    <td>{{ cyc.cycleStart }}</td>
                    <td>{{ cyc.cycleEnd }}</td>
                    <td>
                      <span class="badge" [ngClass]="cyc.status === 'OPEN' ? 'badge-success' : 'badge-danger'">
                        {{ cyc.status }}
                      </span>
                    </td>
                    <td class="action-cell">
                      <!-- Close Cycle -->
                      <button *ngIf="cyc.status === 'OPEN'" (click)="closeCycle(cyc.cycleId)" class="btn btn-secondary btn-small">
                        Close
                      </button>
                    </td>
                  </tr>
                  <tr *ngIf="cycles.length === 0">
                    <td colspan="5" class="text-center muted-text py-20">No billing cycles found.</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <!-- Batch trigger card -->
          <div class="glass-card batch-panel">
            <h3>Trigger Batch Invoicing</h3>
            <p class="description mb-16">Scans all closed cycles eligible for invoicing and generates bills in bulk.</p>
            
            <form [formGroup]="batchForm" (ngSubmit)="onTriggerBatch()" class="mt-16">
              <div class="form-group">
                <label class="form-label">Cutoff Cycle Date</label>
                <input type="date" formControlName="cycleDate" class="form-control">
              </div>
              <div class="form-group flex-row align-center gap-8">
                <input type="checkbox" formControlName="dryRun" id="dryRunCheck">
                <label for="dryRunCheck" class="form-label mb-0">Dry Run (simulation counts only)</label>
              </div>
              <button type="submit" [disabled]="batchForm.invalid" class="btn btn-primary mt-12 btn-block">
                Start Batch Generation
              </button>
            </form>
          </div>
        </div>
      </div>

      <!-- Reports Tab (Billing Specialist / Admin) -->
      <div *ngIf="activeTab === 'reports' && hasReportPermission" class="tab-content animate-fade-in">
        <div class="reports-grid">
          <!-- Overdue Aging bucket report -->
          <div class="glass-card">
            <h3>Overdue Aging Report</h3>
            <div class="mt-16" *ngIf="overdueReport">
              <div class="report-meta-grid">
                <div class="meta-item">
                  <span class="label">Total Overdue Invoices:</span>
                  <span class="value font-bold text-danger">{{ overdueReport.totalOverdueInvoices }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Total Amount Outstanding:</span>
                  <span class="value font-bold text-danger">\${{ overdueReport.totalOverdueAmount | number:'1.2-2' }}</span>
                </div>
              </div>

              <h4 class="mt-20">Aging Buckets</h4>
              <div class="aging-buckets mt-8">
                <div class="bucket" *ngFor="let bucket of getAgingBucketPairs(overdueReport.agingBuckets)">
                  <span class="bucket-name font-bold">{{ bucket.name }} Days:</span>
                  <span class="bucket-val">\${{ bucket.value | number:'1.2-2' }}</span>
                </div>
              </div>
            </div>
            <button (click)="loadOverdueReport()" class="btn btn-secondary mt-16 btn-block">Refresh Aging Buckets</button>
          </div>

          <!-- Collection Efficiency Report -->
          <div class="glass-card">
            <h3>Collection Efficiency</h3>
            <form [formGroup]="collectionForm" (ngSubmit)="loadCollectionReport()" class="mt-16">
              <div class="form-group">
                <label class="form-label">From Date</label>
                <input type="date" formControlName="fromDate" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">To Date</label>
                <input type="date" formControlName="toDate" class="form-control">
              </div>
              <button type="submit" [disabled]="collectionForm.invalid" class="btn btn-primary btn-block">Generate Metrics</button>
            </form>

            <div class="mt-20 animate-fade-in" *ngIf="collectionReport">
              <div class="report-meta-grid">
                <div class="meta-item">
                  <span class="label">Total Invoiced:</span>
                  <span class="value font-bold">\${{ collectionReport.totalInvoiced | number:'1.2-2' }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Total Collected:</span>
                  <span class="value font-bold text-success">\${{ collectionReport.totalCollected | number:'1.2-2' }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Collection Efficiency:</span>
                  <span class="value font-bold text-success">{{ collectionReport.collectionEfficiency | number:'1.0-2' }}%</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Disputes Summary report -->
          <div class="glass-card">
            <h3>Dispute SLA compliance</h3>
            <form [formGroup]="disputesReportForm" (ngSubmit)="loadDisputesSummary()" class="mt-16">
              <div class="form-group">
                <label class="form-label">From Date</label>
                <input type="date" formControlName="fromDate" class="form-control">
              </div>
              <div class="form-group">
                <label class="form-label">To Date</label>
                <input type="date" formControlName="toDate" class="form-control">
              </div>
              <button type="submit" [disabled]="disputesReportForm.invalid" class="btn btn-primary btn-block">Generate Metrics</button>
            </form>

            <div class="mt-20 animate-fade-in" *ngIf="disputeReport">
              <div class="report-meta-grid">
                <div class="meta-item">
                  <span class="label">Total Disputes Raised:</span>
                  <span class="value">{{ disputeReport.totalDisputes }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Resolved Disputes:</span>
                  <span class="value">{{ disputeReport.resolvedDisputes }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Open Disputes:</span>
                  <span class="value">{{ disputeReport.openDisputes }}</span>
                </div>
                <div class="meta-item">
                  <span class="label">Avg Resolution Time:</span>
                  <span class="value font-bold">{{ disputeReport.averageResolutionTimeHours | number:'1.0-1' }} Hours</span>
                </div>
                <div class="meta-item">
                  <span class="label">SLA Compliance:</span>
                  <span class="value font-bold text-success">{{ disputeReport.slaCompliancePercentage | number:'1.0-2' }}%</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Modals -->

    <!-- Pay Bill Modal -->
    <div *ngIf="showPayModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Record Payment for Invoice #{{ selectedInvoice?.invoiceId }}</h2>
        <form [formGroup]="payForm" (ngSubmit)="onConfirmPayment()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Amount to Pay ($)</label>
            <input type="number" step="0.01" formControlName="amountPaid" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Payment Method</label>
            <select formControlName="paymentMethod" class="form-control form-select">
              <option value="UPI">UPI Payment</option>
              <option value="CreditCard">Credit Card</option>
              <option value="DebitCard">Debit Card</option>
              <option value="NetBanking">Net Banking</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Transaction Reference</label>
            <input type="text" formControlName="transactionRef" class="form-control" placeholder="TXN123456789">
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="payForm.invalid" class="btn btn-primary">Pay Invoice</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Raise Dispute Modal -->
    <div *ngIf="showRaiseDisputeModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Raise Dispute on Invoice #{{ selectedInvoice?.invoiceId }}</h2>
        <form [formGroup]="disputeForm" (ngSubmit)="onConfirmDispute()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Reason</label>
            <select formControlName="disputeReason" class="form-control form-select">
              <option value="ExcessData">Excessive Data Charges</option>
              <option value="WrongPlan">Wrong Active Plan Rate</option>
              <option value="LateFeeIncorrect">Late Fee Applied Incorrectly</option>
              <option value="Miscellaneous">Miscellaneous</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Disputed Amount ($)</label>
            <input type="number" step="0.01" formControlName="disputedAmount" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Description Notes</label>
            <textarea formControlName="description" class="form-control" rows="3" placeholder="Provide extra detail..."></textarea>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="disputeForm.invalid" class="btn btn-primary">Raise Dispute</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Apply Late Fee Modal (Staff) -->
    <div *ngIf="showLateFeeModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Apply Late Fee on Invoice #{{ selectedInvoice?.invoiceId }}</h2>
        <form [formGroup]="lateFeeForm" (ngSubmit)="onConfirmLateFee()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Late Fee Amount ($)</label>
            <input type="number" step="0.01" formControlName="feeAmount" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Reason</label>
            <input type="text" formControlName="reason" class="form-control" placeholder="e.g. Overdue past grace period">
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="lateFeeForm.invalid" class="btn btn-primary">Apply Fee</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Waive Late Fee Modal (Staff) -->
    <div *ngIf="showWaiveFeeModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Waive Late Fee on Invoice #{{ selectedInvoice?.invoiceId }}</h2>
        <form [formGroup]="waiveFeeForm" (ngSubmit)="onConfirmWaiveFee()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Waiver Reason</label>
            <input type="text" formControlName="waiverReason" class="form-control" placeholder="e.g. Goodwill gesture">
          </div>
          <div class="form-group">
            <label class="form-label">Authorized By (Staff ID)</label>
            <input type="text" formControlName="authorisedBy" class="form-control">
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="waiveFeeForm.invalid" class="btn btn-primary">Waive Fee</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Review Dispute Modal (Staff) -->
    <div *ngIf="showReviewDisputeModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Review Dispute #{{ selectedDispute?.disputeId }}</h2>
        <form [formGroup]="reviewForm" (ngSubmit)="onConfirmReview()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Assigned Executive ID</label>
            <input type="text" formControlName="assignedTo" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Internal Notes</label>
            <textarea formControlName="notes" class="form-control" rows="3"></textarea>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="reviewForm.invalid" class="btn btn-primary">Under Review</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Resolve Dispute Modal (Staff) -->
    <div *ngIf="showResolveDisputeModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Resolve Dispute #{{ selectedDispute?.disputeId }}</h2>
        <form [formGroup]="resolveForm" (ngSubmit)="onConfirmResolve()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Resolution Code</label>
            <select formControlName="resolution" class="form-control form-select">
              <option value="Resolved_CreditApproved">Resolved - Credit Approved</option>
              <option value="Resolved_CorrectAsBilled">Resolved - Correct As Billed</option>
              <option value="Rejected">Rejected</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Credit Amount Applied ($)</label>
            <input type="number" step="0.01" formControlName="creditAmount" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Resolution Notes</label>
            <textarea formControlName="resolutionNotes" class="form-control" rows="3" placeholder="Provide resolution summary..."></textarea>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="resolveForm.invalid" class="btn btn-primary">Resolve Dispute</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Create Cycle Modal (Staff) -->
    <div *ngIf="showCreateCycleModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Create Billing Cycle</h2>
        <form [formGroup]="cycleForm" (ngSubmit)="onCreateCycle()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Start Date</label>
            <input type="date" formControlName="cycleStart" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">End Date</label>
            <input type="date" formControlName="cycleEnd" class="form-control">
          </div>
          <div class="form-group">
            <label class="form-label">Initial Status</label>
            <select formControlName="status" class="form-control form-select">
              <option value="OPEN">OPEN</option>
              <option value="CLOSED">CLOSED</option>
            </select>
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="cycleForm.invalid" class="btn btn-primary">Create Cycle</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Generate Single Invoice Modal (Staff) -->
    <div *ngIf="showSingleGenerateModal" class="modal-backdrop">
      <div class="glass-card modal-content animate-scale-in">
        <h2>Generate Account Invoice</h2>
        <form [formGroup]="singleGenerateForm" (ngSubmit)="onConfirmSingleGenerate()" class="mt-20">
          <div class="form-group">
            <label class="form-label">Billing Cycle ID</label>
            <input type="number" formControlName="billingCycleId" class="form-control">
          </div>

          <div class="modal-footer mt-24">
            <button type="button" (click)="closeModals()" class="btn btn-secondary">Cancel</button>
            <button type="submit" [disabled]="singleGenerateForm.invalid" class="btn btn-primary">Generate Bill</button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .billing-container {
      display: flex;
      flex-direction: column;
      gap: 32px;
    }
    .billing-header h1 {
      font-size: 2.2rem;
      margin-bottom: 6px;
    }
    .billing-header p {
      color: var(--text-secondary);
      font-size: 1rem;
    }
    .controls-grid {
      display: flex;
      justify-content: space-between;
      align-items: flex-end;
      gap: 20px;
      flex-wrap: wrap;
    }
    .lookup-input-group {
      display: flex;
      gap: 8px;
    }
    .quick-actions {
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
    .cycles-layout {
      display: grid;
      grid-template-columns: 1fr 340px;
      gap: 24px;
    }
    @media (max-width: 992px) {
      .cycles-layout {
        grid-template-columns: 1fr;
      }
    }
    .reports-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
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
    .aging-buckets {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    .bucket {
      display: flex;
      justify-content: space-between;
      border-bottom: 1px solid var(--border-color);
      padding-bottom: 6px;
    }
    .bucket:last-child {
      border-bottom: none;
      padding-bottom: 0;
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
    .flex-wrap {
      flex-wrap: wrap;
    }
  `]
})
export class BillingDashboardComponent implements OnInit {
  private readonly billingService = inject(BillingService);
  private readonly subscriberService = inject(SubscriberService);
  private readonly userService = inject(UserService);
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  selectedAccountId!: number;
  activeTab = 'invoices';

  // Subscriber lists
  subscriberAccounts: AccountResponse[] = [];

  // Data lists
  invoices: InvoiceResponse[] = [];
  disputes: DisputeResponse[] = [];
  cycles: BillingCycleResponse[] = [];

  // Reports data
  overdueReport: OverdueReportResponse | null = null;
  collectionReport: CollectionReportResponse | null = null;
  disputeReport: DisputeSummaryResponse | null = null;

  // Modals state
  showPayModal = false;
  showRaiseDisputeModal = false;
  showLateFeeModal = false;
  showWaiveFeeModal = false;
  showReviewDisputeModal = false;
  showResolveDisputeModal = false;
  showCreateCycleModal = false;
  showSingleGenerateModal = false;

  selectedInvoice: InvoiceResponse | null = null;
  selectedDispute: DisputeResponse | null = null;

  // Forms
  payForm!: FormGroup;
  disputeForm!: FormGroup;
  lateFeeForm!: FormGroup;
  waiveFeeForm!: FormGroup;
  reviewForm!: FormGroup;
  resolveForm!: FormGroup;
  cycleForm!: FormGroup;
  batchForm!: FormGroup;
  collectionForm!: FormGroup;
  disputesReportForm!: FormGroup;
  singleGenerateForm!: FormGroup;

  // Alerts
  alertMessage = '';
  successMessage = '';
  errorMessage = '';

  get isSubscriber(): boolean {
    return this.authService.userRole() === 'S';
  }

  get isStaff(): boolean {
    return this.authService.userRole() === 'A' || this.authService.userRole() === 'B' || this.authService.userRole() === 'CS';
  }

  get hasReportPermission(): boolean {
    return this.authService.hasPermission('BILLING_REPORT');
  }

  ngOnInit() {
    this.initForms();
    this.loadSubscriberAccounts();
  }

  setTab(tab: string) {
    this.activeTab = tab;
    this.clearAlerts();
    this.loadTabDetails();
  }

  private initForms() {
    const todayStr = new Date().toISOString().split('T')[0];

    this.payForm = this.fb.group({
      amountPaid: [0, [Validators.required, Validators.min(0.01)]],
      paymentMethod: ['UPI', Validators.required],
      transactionRef: ['', Validators.required]
    });

    this.disputeForm = this.fb.group({
      disputeReason: ['ExcessData', Validators.required],
      disputedAmount: [0, [Validators.required, Validators.min(0.01)]],
      description: ['', Validators.required]
    });

    this.lateFeeForm = this.fb.group({
      feeAmount: [10, [Validators.required, Validators.min(0.01)]],
      reason: ['Overdue past due date grace period', Validators.required]
    });

    this.waiveFeeForm = this.fb.group({
      waiverReason: ['Customer goodwill waiver request', Validators.required],
      authorisedBy: ['billing-specialist-1', Validators.required]
    });

    this.reviewForm = this.fb.group({
      assignedTo: ['billing-exec-1', Validators.required],
      notes: ['', Validators.required]
    });

    this.resolveForm = this.fb.group({
      resolution: ['Resolved_CreditApproved', Validators.required],
      creditAmount: [0, Validators.required],
      resolutionNotes: ['', Validators.required]
    });

    this.cycleForm = this.fb.group({
      cycleStart: [todayStr, Validators.required],
      cycleEnd: [todayStr, Validators.required],
      status: ['OPEN', Validators.required]
    });

    this.batchForm = this.fb.group({
      cycleDate: [todayStr, Validators.required],
      dryRun: [false]
    });

    this.collectionForm = this.fb.group({
      fromDate: [todayStr, Validators.required],
      toDate: [todayStr, Validators.required]
    });

    this.disputesReportForm = this.fb.group({
      fromDate: [todayStr, Validators.required],
      toDate: [todayStr, Validators.required]
    });

    this.singleGenerateForm = this.fb.group({
      billingCycleId: ['', Validators.required]
    });
  }

  private clearAlerts() {
    this.alertMessage = '';
    this.successMessage = '';
    this.errorMessage = '';
  }

  private loadSubscriberAccounts() {
    if (!this.isSubscriber) return;

    this.userService.getMe().subscribe({
      next: (user) => {
        this.subscriberService.getAllAccounts(undefined, user.userId).subscribe({
          next: (res) => {
            this.subscriberAccounts = res.subscribers || [];
            if (this.subscriberAccounts.length > 0) {
              this.selectedAccountId = this.subscriberAccounts[0].accountId;
              this.onAccountChange();
            }
          }
        });
      }
    });
  }

  onAccountChange() {
    if (!this.selectedAccountId) return;
    this.clearAlerts();
    this.loadTabDetails();
  }

  private loadTabDetails() {
    if (!this.selectedAccountId) return;

    if (this.activeTab === 'invoices') {
      this.billingService.getInvoicesByAccount(Number(this.selectedAccountId)).subscribe({
        next: (res) => this.invoices = res,
        error: () => this.invoices = []
      });
    }

    if (this.activeTab === 'disputes') {
      this.billingService.getDisputesByAccount(Number(this.selectedAccountId)).subscribe({
        next: (res) => this.disputes = res,
        error: () => this.disputes = []
      });
    }

    if (this.activeTab === 'cycles' && this.isStaff) {
      this.billingService.getCyclesByAccount(Number(this.selectedAccountId)).subscribe({
        next: (res) => this.cycles = res,
        error: () => this.cycles = []
      });
    }

    if (this.activeTab === 'reports' && this.hasReportPermission) {
      this.loadOverdueReport();
    }
  }

  getInvoiceBadgeClass(status: string): string {
    switch (status) {
      case 'PAID': return 'badge-success';
      case 'OVERDUE': return 'badge-danger';
      case 'DISPUTED': return 'badge-warning';
      case 'SENT': return 'badge-info';
      default: return 'badge-secondary';
    }
  }

  getDisputeBadgeClass(status: string): string {
    switch (status) {
      case 'RESOLVED': return 'badge-success';
      case 'REJECTED': return 'badge-danger';
      case 'UNDER_REVIEW': return 'badge-warning';
      default: return 'badge-info';
    }
  }

  // Download Invoice PDF
  downloadInvoice(invoiceId: number) {
    this.billingService.downloadInvoicePdf(invoiceId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Invoice_${invoiceId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.errorMessage = 'Failed to download invoice PDF.'
    });
  }

  // Download Account Statement PDF
  downloadStatement(accountId: number) {
    this.billingService.downloadAccountStatementPdf(accountId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `Statement_Account_${accountId}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: () => this.errorMessage = 'Failed to download statement.'
    });
  }

  // Pay Invoice
  openPayModal(inv: InvoiceResponse) {
    this.selectedInvoice = inv;
    const balance = inv.totalAmount - inv.amountPaid;
    this.payForm.patchValue({
      amountPaid: balance,
      transactionRef: 'TXN' + Math.floor(Math.random() * 100000000)
    });
    this.showPayModal = true;
  }

  onConfirmPayment() {
    if (this.payForm.invalid || !this.selectedInvoice) return;
    this.billingService.payInvoice(this.selectedInvoice.invoiceId, this.payForm.value).subscribe({
      next: () => {
        this.successMessage = 'Payment recorded successfully!';
        this.closeModals();
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to record payment.'
    });
  }

  // Raise Dispute
  openRaiseDisputeModal(inv: InvoiceResponse) {
    this.selectedInvoice = inv;
    this.disputeForm.patchValue({
      disputedAmount: inv.totalAmount - inv.amountPaid,
      description: ''
    });
    this.showRaiseDisputeModal = true;
  }

  onConfirmDispute() {
    if (this.disputeForm.invalid || !this.selectedInvoice) return;
    const req = {
      invoiceId: this.selectedInvoice.invoiceId,
      ...this.disputeForm.value
    };
    this.billingService.raiseDispute(req).subscribe({
      next: () => {
        this.successMessage = 'Dispute raised successfully!';
        this.closeModals();
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to raise dispute.'
    });
  }

  // Send Invoice
  sendInvoice(invoiceId: number) {
    this.billingService.sendInvoice(invoiceId).subscribe({
      next: () => {
        this.successMessage = 'Invoice sent to subscriber successfully!';
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to send invoice.'
    });
  }

  // Apply Late Fee
  openLateFeeModal(inv: InvoiceResponse) {
    this.selectedInvoice = inv;
    this.lateFeeForm.patchValue({ feeAmount: 10 });
    this.showLateFeeModal = true;
  }

  onConfirmLateFee() {
    if (this.lateFeeForm.invalid || !this.selectedInvoice) return;
    this.billingService.applyLateFee(this.selectedInvoice.invoiceId, this.lateFeeForm.value).subscribe({
      next: () => {
        this.successMessage = 'Late fee applied successfully!';
        this.closeModals();
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to apply late fee.'
    });
  }

  // Waive Late Fee
  openWaiveFeeModal(inv: InvoiceResponse) {
    this.selectedInvoice = inv;
    this.showWaiveFeeModal = true;
  }

  onConfirmWaiveFee() {
    if (this.waiveFeeForm.invalid || !this.selectedInvoice) return;
    this.billingService.waiveLateFee(this.selectedInvoice.invoiceId, this.waiveFeeForm.value).subscribe({
      next: () => {
        this.successMessage = 'Late fee waived successfully!';
        this.closeModals();
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to waive late fee.'
    });
  }

  // Review Dispute
  openReviewDisputeModal(disp: DisputeResponse) {
    this.selectedDispute = disp;
    this.reviewForm.patchValue({ assignedTo: 'exec-1', notes: '' });
    this.showReviewDisputeModal = true;
  }

  onConfirmReview() {
    if (this.reviewForm.invalid || !this.selectedDispute) return;
    this.billingService.reviewDispute(this.selectedDispute.disputeId, this.reviewForm.value).subscribe({
      next: () => {
        this.successMessage = 'Dispute status updated to UNDER_REVIEW!';
        this.closeModals();
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to review dispute.'
    });
  }

  // Resolve Dispute
  openResolveDisputeModal(disp: DisputeResponse) {
    this.selectedDispute = disp;
    this.resolveForm.patchValue({
      resolution: 'Resolved_CreditApproved',
      creditAmount: disp.disputedAmount,
      resolutionNotes: ''
    });
    this.showResolveDisputeModal = true;
  }

  onConfirmResolve() {
    if (this.resolveForm.invalid || !this.selectedDispute) return;
    this.billingService.resolveDispute(this.selectedDispute.disputeId, this.resolveForm.value).subscribe({
      next: () => {
        this.successMessage = 'Dispute resolved successfully!';
        this.closeModals();
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to resolve dispute.'
    });
  }

  // Create Cycle
  onCreateCycle() {
    if (this.cycleForm.invalid || !this.selectedAccountId) return;
    const req = {
      accountId: Number(this.selectedAccountId),
      ...this.cycleForm.value
    };
    this.billingService.createBillingCycle(req).subscribe({
      next: () => {
        this.successMessage = 'Billing Cycle created successfully!';
        this.closeModals();
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to create billing cycle.'
    });
  }

  closeCycle(cycleId: number) {
    this.billingService.closeCycle(cycleId).subscribe({
      next: () => {
        this.successMessage = 'Billing cycle closed successfully!';
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to close cycle.'
    });
  }

  // Generate Single Invoice
  openSingleGenerateModal() {
    this.singleGenerateForm.reset({ billingCycleId: '' });
    this.showSingleGenerateModal = true;
  }

  onConfirmSingleGenerate() {
    if (this.singleGenerateForm.invalid || !this.selectedAccountId) return;
    const req = {
      accountId: Number(this.selectedAccountId),
      billingCycleId: Number(this.singleGenerateForm.value.billingCycleId)
    };
    this.billingService.generateInvoice(req).subscribe({
      next: () => {
        this.successMessage = 'Invoice generated successfully!';
        this.closeModals();
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to generate invoice.'
    });
  }

  // Batch invoicing
  onTriggerBatch() {
    if (this.batchForm.invalid) return;
    this.billingService.generateInvoicesBatch(this.batchForm.value).subscribe({
      next: () => {
        this.successMessage = 'Batch invoice generation completed successfully!';
        this.loadTabDetails();
      },
      error: (err) => this.errorMessage = err.error?.message || 'Failed to generate batch invoices.'
    });
  }

  // Mark overdue batch
  triggerMarkOverdue() {
    this.billingService.markOverdue().subscribe({
      next: () => {
        this.successMessage = 'Overdue invoices status scan completed successfully!';
        this.loadTabDetails();
      },
      error: () => this.errorMessage = 'Failed to mark overdue invoices.'
    });
  }

  // Reports
  loadOverdueReport() {
    this.billingService.getOverdueReport().subscribe({
      next: (res) => this.overdueReport = res,
      error: () => this.overdueReport = null
    });
  }

  loadCollectionReport() {
    if (this.collectionForm.invalid) return;
    const { fromDate, toDate } = this.collectionForm.value;
    this.billingService.getCollectionReport(fromDate, toDate).subscribe({
      next: (res) => this.collectionReport = res,
      error: () => this.collectionReport = null
    });
  }

  loadDisputesSummary() {
    if (this.disputesReportForm.invalid) return;
    const { fromDate, toDate } = this.disputesReportForm.value;
    this.billingService.getDisputeSummaryReport(fromDate, toDate).subscribe({
      next: (res) => this.disputeReport = res,
      error: () => this.disputeReport = null
    });
  }

  getAgingBucketPairs(buckets: any): Array<{ name: string, value: number }> {
    if (!buckets) return [];
    return Object.keys(buckets).map(key => ({ name: key, value: buckets[key] }));
  }

  closeModals() {
    this.showPayModal = false;
    this.showRaiseDisputeModal = false;
    this.showLateFeeModal = false;
    this.showWaiveFeeModal = false;
    this.showReviewDisputeModal = false;
    this.showResolveDisputeModal = false;
    this.showCreateCycleModal = false;
    this.showSingleGenerateModal = false;
    this.selectedInvoice = null;
    this.selectedDispute = null;
  }
}
