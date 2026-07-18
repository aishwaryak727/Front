-- TeleConnect Module 2.7 — Telecom Analytics & Reporting
-- DDL for the ONLY table this module owns. Run once against teleconnect_db.
--
-- IMPORTANT (post API-consumer refactor): this service no longer maps
-- or queries subscriber_accounts, sim_lines, billing_cycles, invoices,
-- billing_disputes, usage_summaries, or fault_tickets directly. Those
-- tables belong to their respective owning modules (Subscriber & SIM,
-- Billing & Invoice, Usage & Consumption, Fault & Service Request) and
-- are now reached exclusively through each module's analytics-facing
-- REST API (see client/ package and the teleconnect.modules.* base-url
-- properties in application.properties). Do not re-add those CREATE
-- TABLE statements here — analytics-service is no longer their owner.

CREATE TABLE IF NOT EXISTS telecom_reports (
    report_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    scope           ENUM('REGION','PLAN','SEGMENT','PERIOD') NOT NULL,
    scope_value     VARCHAR(100) NOT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    metrics         TEXT NOT NULL,
    generated_date  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    generated_by    BIGINT,
    INDEX idx_report_scope_period (scope, period_start, period_end)
);
