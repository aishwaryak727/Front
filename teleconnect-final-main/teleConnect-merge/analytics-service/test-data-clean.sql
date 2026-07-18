-- ============================================================================
-- TeleConnect Analytics Service - Complete Test Data Setup
-- ============================================================================
-- This single SQL file contains all test data needed for all peer services.
-- Execute the relevant sections in each service's database.
--
-- Services & Ports:
-- - Subscriber Service (8081) → teleconnect_subscriber_db
-- - Billing Service (8082) → teleconnect_billing_db
-- - Usage Service (8083) → teleconnect_usage_db
-- - Fault Service (8084) → teleconnect_fault_db
-- - Analytics Service (8089) → teleconnect_db
-- ============================================================================

-- ============================================================================
-- SUBSCRIBER SERVICE - Execute in teleconnect_subscriber_db
-- ============================================================================

-- CREATE TABLES
CREATE TABLE IF NOT EXISTS subscriber_accounts (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscriber_id BIGINT NOT NULL,
    account_type ENUM('PREPAID', 'POSTPAID', 'ENTERPRISE') NOT NULL,
    registration_date DATE NOT NULL,
    region_id BIGINT,
    status ENUM('ACTIVE', 'SUSPENDED', 'TERMINATED') NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_subscriber_id (subscriber_id),
    INDEX idx_status (status),
    INDEX idx_region (region_id)
);

CREATE TABLE IF NOT EXISTS sim_lines (
    line_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    activation_date DATE NOT NULL,
    status ENUM('ACTIVE', 'DEACTIVATED', 'SUSPENDED', 'PORTED_OUT') NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES subscriber_accounts(account_id),
    INDEX idx_account_id (account_id),
    INDEX idx_status (status)
);

-- Clear existing data (optional - uncomment if you want to reset)
-- TRUNCATE TABLE sim_lines;
-- TRUNCATE TABLE subscriber_accounts;

-- Insert Subscriber Accounts (ACTIVE - for base metrics)
INSERT IGNORE INTO subscriber_accounts (account_id, subscriber_id, account_type, registration_date, region_id, status) VALUES
(1001, 5001, 'PREPAID', '2024-01-15', 1, 'ACTIVE'),
(1002, 5002, 'POSTPAID', '2024-02-20', 1, 'ACTIVE'),
(1003, 5003, 'ENTERPRISE', '2024-03-10', 2, 'ACTIVE'),
(1004, 5004, 'PREPAID', '2024-01-05', 2, 'ACTIVE'),
(1005, 5005, 'POSTPAID', '2024-04-01', 1, 'ACTIVE'),
(1006, 5006, 'PREPAID', '2024-05-15', 2, 'ACTIVE'),
(1007, 5007, 'POSTPAID', '2024-02-10', 1, 'ACTIVE'),
(1008, 5008, 'ENTERPRISE', '2024-03-20', 2, 'ACTIVE');

-- Insert Subscriber Accounts (TERMINATED - for churn calculation)
INSERT IGNORE INTO subscriber_accounts (account_id, subscriber_id, account_type, registration_date, region_id, status) VALUES
(1009, 5009, 'PREPAID', '2023-06-01', 1, 'TERMINATED'),
(1010, 5010, 'POSTPAID', '2023-07-15', 2, 'TERMINATED'),
(1011, 5011, 'PREPAID', '2023-08-20', 1, 'TERMINATED');

-- Insert SIM Lines (ACTIVE)
INSERT IGNORE INTO sim_lines (line_id, account_id, activation_date, status) VALUES
(2001, 1001, '2024-01-15', 'ACTIVE'),
(2002, 1002, '2024-02-20', 'ACTIVE'),
(2003, 1003, '2024-03-10', 'ACTIVE'),
(2004, 1004, '2024-01-05', 'ACTIVE'),
(2005, 1005, '2024-04-01', 'ACTIVE'),
(2006, 1006, '2024-05-15', 'ACTIVE'),
(2007, 1007, '2024-02-10', 'ACTIVE'),
(2008, 1008, '2024-03-20', 'ACTIVE');

-- Insert SIM Lines (PORTED_OUT - for churn calculation)
INSERT IGNORE INTO sim_lines (line_id, account_id, activation_date, status) VALUES
(2009, 1009, '2023-06-01', 'PORTED_OUT'),
(2010, 1010, '2023-07-15', 'PORTED_OUT');

-- ============================================================================
-- BILLING SERVICE - Execute in teleconnect_billing_db
-- ============================================================================

-- CREATE TABLES
CREATE TABLE IF NOT EXISTS invoices (
    invoice_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    cycle_id BIGINT NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    paid_amount DECIMAL(12, 2) DEFAULT 0.00,
    due_date DATE NOT NULL,
    status ENUM('GENERATED', 'SENT', 'PAID', 'OVERDUE', 'DISPUTED') NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_id (account_id),
    INDEX idx_cycle_id (cycle_id),
    INDEX idx_status (status)
);

CREATE TABLE IF NOT EXISTS billing_disputes (
    dispute_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscriber_id BIGINT NOT NULL,
    invoice_id BIGINT,
    status ENUM('OPEN', 'UNDER_REVIEW', 'RESOLVED') NOT NULL,
    raised_date DATETIME NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id),
    INDEX idx_subscriber_id (subscriber_id),
    INDEX idx_status (status)
);

-- Clear existing data (optional - uncomment if you want to reset)
-- TRUNCATE TABLE billing_disputes;
-- TRUNCATE TABLE invoices;

-- Insert Invoices (PAID - for collection efficiency)
INSERT IGNORE INTO invoices (invoice_id, account_id, cycle_id, total_amount, paid_amount, due_date, status) VALUES
(3001, 1001, 1, 1500.00, 1500.00, '2024-06-30', 'PAID'),
(3002, 1002, 1, 2500.00, 2500.00, '2024-06-30', 'PAID'),
(3003, 1003, 1, 5000.00, 5000.00, '2024-06-30', 'PAID'),
(3004, 1005, 1, 1800.00, 1800.00, '2024-06-30', 'PAID'),
(3005, 1007, 1, 2200.00, 2200.00, '2024-06-30', 'PAID');

-- Insert Invoices (OVERDUE - for collection efficiency & at-risk detection)
INSERT IGNORE INTO invoices (invoice_id, account_id, cycle_id, total_amount, paid_amount, due_date, status) VALUES
(3006, 1004, 1, 1200.00, 0.00, '2024-05-15', 'OVERDUE'),
(3007, 1006, 1, 1600.00, 0.00, '2024-05-20', 'OVERDUE'),
(3008, 1008, 1, 4500.00, 2000.00, '2024-06-10', 'OVERDUE');

-- Insert Invoices (SENT - for ARPU calculation)
INSERT IGNORE INTO invoices (invoice_id, account_id, cycle_id, total_amount, paid_amount, due_date, status) VALUES
(3009, 1001, 2, 1500.00, 0.00, '2024-07-31', 'SENT'),
(3010, 1002, 2, 2500.00, 0.00, '2024-07-31', 'SENT'),
(3011, 1003, 2, 5000.00, 0.00, '2024-07-31', 'SENT'),
(3012, 1004, 2, 1200.00, 0.00, '2024-07-31', 'SENT'),
(3013, 1005, 2, 1800.00, 0.00, '2024-07-31', 'SENT'),
(3014, 1006, 2, 1600.00, 0.00, '2024-07-31', 'SENT'),
(3015, 1007, 2, 2200.00, 0.00, '2024-07-31', 'SENT'),
(3016, 1008, 2, 4500.00, 0.00, '2024-07-31', 'SENT');

-- Insert Billing Disputes (for at-risk account detection)
INSERT IGNORE INTO billing_disputes (dispute_id, subscriber_id, invoice_id, status, raised_date) VALUES
(4001, 5004, 3006, 'OPEN', '2024-06-01'),
(4002, 5006, 3007, 'UNDER_REVIEW', '2024-06-05');

-- ============================================================================
-- USAGE SERVICE - Execute in teleconnect_usage_db
-- ============================================================================

-- CREATE TABLE
CREATE TABLE IF NOT EXISTS usage_summaries (
    summary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    line_id BIGINT NOT NULL,
    billing_cycle_id BIGINT NOT NULL,
    data_used_mb BIGINT DEFAULT 0,
    voice_used_min BIGINT DEFAULT 0,
    sms_used BIGINT DEFAULT 0,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_line_id (line_id),
    INDEX idx_cycle_id (billing_cycle_id)
);

-- Clear existing data (optional - uncomment if you want to reset)
-- TRUNCATE TABLE usage_summaries;

-- Insert Usage Summaries for Cycle 1 (June 2024)
INSERT IGNORE INTO usage_summaries (summary_id, line_id, billing_cycle_id, data_used_mb, voice_used_min, sms_used) VALUES
(5001, 2001, 1, 1200, 300, 100),
(5002, 2002, 1, 2500, 600, 250),
(5003, 2003, 1, 5000, 1200, 500),
(5004, 2004, 1, 800, 250, 75),
(5005, 2005, 1, 1500, 400, 120),
(5006, 2006, 1, 900, 200, 80),
(5007, 2007, 1, 2000, 500, 180),
(5008, 2008, 1, 4500, 1000, 450);

-- Insert Usage Summaries for Cycle 2 (July 2024)
INSERT IGNORE INTO usage_summaries (summary_id, line_id, billing_cycle_id, data_used_mb, voice_used_min, sms_used) VALUES
(5009, 2001, 2, 1100, 280, 95),
(5010, 2002, 2, 2600, 620, 260),
(5011, 2003, 2, 5100, 1250, 520),
(5012, 2004, 2, 750, 220, 70),
(5013, 2005, 2, 1600, 420, 130),
(5014, 2006, 2, 850, 180, 75),
(5015, 2007, 2, 2100, 520, 190),
(5016, 2008, 2, 4600, 1050, 460);

-- ============================================================================
-- FAULT SERVICE - Execute in teleconnect_fault_db
-- ============================================================================

-- CREATE TABLE
CREATE TABLE IF NOT EXISTS fault_tickets (
    ticket_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    line_id BIGINT,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL,
    raised_date DATETIME NOT NULL,
    resolved_date DATETIME,
    status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'ESCALATED') NOT NULL,
    created_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_id (account_id),
    INDEX idx_priority (priority),
    INDEX idx_status (status)
);

-- Clear existing data (optional - uncomment if you want to reset)
-- TRUNCATE TABLE fault_tickets;

-- Insert Fault Tickets (RESOLVED - for SLA compliance)
INSERT IGNORE INTO fault_tickets (ticket_id, account_id, line_id, priority, raised_date, resolved_date, status) VALUES
(6001, 1001, 2001, 'HIGH', '2024-06-01 10:00:00', '2024-06-01 14:00:00', 'RESOLVED'),
(6002, 1002, 2002, 'CRITICAL', '2024-06-05 09:00:00', '2024-06-05 11:00:00', 'RESOLVED'),
(6003, 1003, 2003, 'MEDIUM', '2024-06-10 08:00:00', '2024-06-10 20:00:00', 'RESOLVED'),
(6004, 1005, 2005, 'LOW', '2024-06-15 15:00:00', '2024-06-18 09:00:00', 'RESOLVED'),
(6005, 1007, 2007, 'HIGH', '2024-06-20 12:00:00', '2024-06-20 18:00:00', 'RESOLVED');

-- Insert Fault Tickets (CLOSED - for SLA compliance)
INSERT IGNORE INTO fault_tickets (ticket_id, account_id, line_id, priority, raised_date, resolved_date, status) VALUES
(6006, 1006, 2006, 'MEDIUM', '2024-06-08 11:00:00', '2024-06-09 10:00:00', 'CLOSED'),
(6007, 1008, 2008, 'HIGH', '2024-06-12 07:00:00', '2024-06-12 16:00:00', 'CLOSED');

-- Insert Fault Tickets (OPEN - for at-risk account detection)
INSERT IGNORE INTO fault_tickets (ticket_id, account_id, line_id, priority, raised_date, resolved_date, status) VALUES
(6008, 1004, 2004, 'HIGH', '2024-06-25 14:00:00', NULL, 'OPEN'),
(6009, 1004, 2004, 'MEDIUM', '2024-06-26 10:00:00', NULL, 'OPEN'),
(6010, 1006, 2006, 'LOW', '2024-06-27 13:00:00', NULL, 'OPEN');

-- Insert Fault Tickets for July (for broader testing)
INSERT IGNORE INTO fault_tickets (ticket_id, account_id, line_id, priority, raised_date, resolved_date, status) VALUES
(6011, 1001, 2001, 'MEDIUM', '2024-07-01 09:00:00', '2024-07-01 14:00:00', 'RESOLVED'),
(6012, 1002, 2002, 'HIGH', '2024-07-05 10:00:00', '2024-07-05 15:00:00', 'CLOSED'),
(6013, 1003, 2003, 'CRITICAL', '2024-07-10 08:00:00', '2024-07-10 10:00:00', 'RESOLVED');

-- ============================================================================
-- ANALYTICS SERVICE - Execute in teleconnect_db (local database)
-- ============================================================================

-- Optional: Pre-populate one sample report if needed
INSERT IGNORE INTO telecom_reports (scope, scope_value, period_start, period_end, metrics, generated_by) VALUES
('PERIOD', 'JUNE_2024', '2024-06-01', '2024-06-30', 
 '{"ChurnRate":2.5,"GrossChurned":2,"AtRiskCount":2,"FaultResolutionRate":100.0,"SLABreachCount":0,"AvgResolutionHours":15.5,"ActiveSubscribers":8,"NetAdds":5}',
 1);

-- ============================================================================
-- END OF TEST DATA
-- ============================================================================
-- Summary of test data:
-- - 8 ACTIVE subscriber accounts, 3 TERMINATED (for churn)
-- - 8 ACTIVE SIM lines, 2 PORTED_OUT (for churn)
-- - 13 invoices: 5 PAID, 3 OVERDUE, 5 SENT (for ARPU & collection efficiency)
-- - 2 billing disputes (for at-risk detection)
-- - 16 usage summaries across 2 cycles (for network utilization)
-- - 13 fault tickets: 5 RESOLVED, 2 CLOSED, 6 OPEN (for SLA compliance & at-risk)
-- 
-- Key Test Scenarios:
-- 1. ARPU Calculation: Can calculate revenue per user across account types
-- 2. Churn Analysis: Has terminated accounts and ported-out lines
-- 3. SLA Compliance: Has fault tickets across all priority levels
-- 4. Network Utilization: Has usage data across data, voice, SMS
-- 5. Collection Efficiency: Has paid, overdue, and sent invoices
-- 6. Subscriber Growth: Can track new activations vs terminations
-- 7. At-Risk Accounts: Account 1004 has 2+ open tickets + dispute; Account 1006 has dispute + open ticket
-- ============================================================================
