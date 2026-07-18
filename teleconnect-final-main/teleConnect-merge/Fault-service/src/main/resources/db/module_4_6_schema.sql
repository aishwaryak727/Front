-- ============================================================================
-- TeleConnect · Module 2.6 / 4.6 — Fault & Service Request Management
-- Schema + sample data for the `service_request` and `fault_ticket` tables.
-- Run once in MySQL Workbench against the `teleconnect` database.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS teleconnect;
USE teleconnect;

-- ── service_request ─────────────────────────────────────────────────────────
-- Status: O=Open  P=InProgress  C=Completed  X=Cancelled
CREATE TABLE IF NOT EXISTS service_request (
    requestId    INT AUTO_INCREMENT PRIMARY KEY,
    accountId    INT NOT NULL,                       -- from Module 2.2
    lineId       INT NOT NULL,                       -- from Module 2.2
    requestType  ENUM('PlanChange','SIMReplacement','PortingRequest','AccountUpdate') NOT NULL,
    requestedBy  INT NOT NULL,                        -- from Module 4.1
    raisedDate   DATE NOT NULL,
    status       ENUM('O','P','C','X') NOT NULL DEFAULT 'O'
);

-- ── fault_ticket ─────────────────────────────────────────────────────────────
-- Priority: L=Low  M=Medium  H=High  C=Critical
-- Status:   O=Open  P=InProgress  R=Resolved  C=Closed  E=Escalated
CREATE TABLE IF NOT EXISTS fault_ticket (
    ticketId      INT AUTO_INCREMENT PRIMARY KEY,
    accountId     INT NOT NULL,                       -- from Module 2.2
    lineId        INT NOT NULL,                       -- from Module 2.2
    faultType     ENUM('NoCoverage','CallDrops','SlowData','BillingIssue','Activation') NOT NULL,
    description   VARCHAR(500) NOT NULL,
    priority      ENUM('L','M','H','C') NOT NULL DEFAULT 'M',
    raisedDate    DATE NOT NULL,
    resolvedDate  DATE NULL,                           -- only set when status = R
    assignedToId  INT NULL,                            -- from Module 4.1; null until assigned
    status        ENUM('O','P','R','C','E') NOT NULL DEFAULT 'O'
);

-- ── Sample data ──────────────────────────────────────────────────────────────
INSERT INTO service_request (accountId, lineId, requestType, requestedBy, raisedDate, status) VALUES
    (1, 1, 'PlanChange',      1, '2026-06-01', 'O'),
    (1, 2, 'SIMReplacement',  1, '2026-06-02', 'P'),
    (2, 3, 'PortingRequest',  2, '2026-06-03', 'O'),
    (2, 3, 'AccountUpdate',   2, '2026-06-04', 'C'),
    (3, 4, 'PlanChange',      3, '2026-06-05', 'X');

INSERT INTO fault_ticket (accountId, lineId, faultType, description, priority, raisedDate, resolvedDate, assignedToId, status) VALUES
    (1, 1, 'SlowData',     'Mobile data very slow since morning', 'H', '2026-06-01', NULL,         10,   'O'),
    (1, 2, 'CallDrops',    'Calls dropping in city centre',        'M', '2026-06-02', NULL,         NULL, 'P'),
    (2, 3, 'NoCoverage',   'No signal at home address',            'C', '2026-06-03', NULL,         11,   'E'),
    (2, 3, 'Activation',   'New SIM not activating',               'H', '2026-06-04', '2026-06-06', 10,   'R'),
    (3, 4, 'BillingIssue', 'Charged for add-on not purchased',     'L', '2026-06-05', NULL,         NULL, 'O');
