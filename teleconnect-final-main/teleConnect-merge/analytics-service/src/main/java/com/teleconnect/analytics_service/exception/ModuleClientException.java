package com.teleconnect.analytics_service.exception;

/**
 * Thrown when a call to a peer module's analytics API fails (timeout,
 * connection refused, non-2xx response, or unparseable body). Distinct
 * from AnalyticsException so callers/handlers can decide whether to
 * fail the whole request or degrade gracefully (e.g. mark that
 * module's contribution to a composite report as "unavailable").
 */
public class ModuleClientException extends RuntimeException {

    private final String moduleName;

    public ModuleClientException(String moduleName, String message, Throwable cause) {
        super("[" + moduleName + "] " + message, cause);
        this.moduleName = moduleName;
    }

    public ModuleClientException(String moduleName, String message) {
        super("[" + moduleName + "] " + message);
        this.moduleName = moduleName;
    }

    public String getModuleName() { return moduleName; }
}
