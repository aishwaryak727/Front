package com.teleconnect.common.audit;

/**
 * The originating module of an audited action. Stored in audit_logs.module.
 */
public enum AuditModule {
    IAM,
    SUBSCRIBER,
    PLAN,
    USAGE,
    BILLING,
    NOTIFICATION,
    FAULT,
    ANALYTICS
}
