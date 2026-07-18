package com.teleconnect.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Small helper for reading the current authenticated principal.
 *
 * <p>Note: the TeleConnect JWT carries the user's email as the principal name,
 * not the numeric userId. The numeric userId is resolved centrally by IAM when
 * it writes the audit row.</p>
 */
public final class SecurityContextUtil {

    private SecurityContextUtil() {
    }

    /** @return the authenticated principal name (email), or null if unauthenticated. */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : null;
    }

    /**
     * Kept for API parity with the audit template. Returns the principal name
     * (email); the numeric id is resolved by IAM from the forwarded JWT.
     */
    public static String getCurrentUserId() {
        return getCurrentUsername();
    }
}
