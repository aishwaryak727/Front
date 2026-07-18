package com.teleconnect.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Gateway JWT utility for validating tokens issued by IAM.
 * Uses the same secret key as IAM to verify token integrity.
 */
@Component
public class JwtUtil {

    private final SecretKey key;

    public JwtUtil(@Value("${jwt.secret:TeleConnect_Super_Secret_Key_2024_IAM_Min32Chars_XYZ}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Validates JWT token signature and expiration.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts email/username from token claims.
     */
    public String extractEmail(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts permissions list from token claims.
     */
    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        try {
            return (List<String>) Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get("permissions", List.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
