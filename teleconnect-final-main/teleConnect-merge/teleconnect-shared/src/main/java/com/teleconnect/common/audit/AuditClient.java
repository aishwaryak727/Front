package com.teleconnect.common.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends an audit record to the IAM audit endpoint over HTTP.
 *
 * <p>The caller's JWT is forwarded so IAM authenticates the same principal and
 * resolves the numeric userId from the token's email. Auditing is best-effort:
 * any failure is logged and swallowed so it never breaks the business operation.</p>
 */
@Slf4j
public class AuditClient {

    private final RestClient restClient;
    private final String auditUrl;

    public AuditClient(String auditUrl) {
        this.auditUrl = auditUrl;
        this.restClient = RestClient.create();
    }

    /**
     * Convenience overload: pulls the Authorization header and client IP straight
     * from the incoming request. Call this AFTER the business operation succeeds.
     */
    public void record(AuditAction action, AuditModule module, HttpServletRequest request) {
        String authHeader = request != null ? request.getHeader(HttpHeaders.AUTHORIZATION) : null;
        record(action, module, extractClientIp(request), authHeader);
    }

    public void record(AuditAction action, AuditModule module, String ipAddress, String authHeader) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("action", action != null ? action.name() : null);
            body.put("module", module != null ? module.name() : null);
            body.put("ipAddress", ipAddress);

            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(auditUrl)
                    .contentType(MediaType.APPLICATION_JSON);
            if (authHeader != null && !authHeader.isBlank()) {
                spec = spec.header(HttpHeaders.AUTHORIZATION, authHeader);
            }
            spec.body(body).retrieve().toBodilessEntity();
        } catch (Exception e) {
            // Best-effort: never let an audit failure break the caller's request.
            log.warn("Audit record failed for action={} module={}: {}", action, module, e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
