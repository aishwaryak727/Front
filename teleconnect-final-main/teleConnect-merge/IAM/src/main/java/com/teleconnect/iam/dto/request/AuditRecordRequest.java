package com.teleconnect.iam.dto.request;

/**
 * Inbound payload for recording an audit entry from any module.
 * The acting user's numeric id is NOT supplied here — IAM resolves it from
 * the authenticated principal (the JWT's email) at write time.
 */
public class AuditRecordRequest {

    private String action;     // e.g. CREATE_PLAN
    private String module;     // e.g. PLAN
    private String ipAddress;  // original caller IP, captured by the calling module

    public AuditRecordRequest() {}

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
