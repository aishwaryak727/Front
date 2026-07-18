package com.teleconnect.subscriber.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.*;

@Component
public class JwtUtil {

    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.expiration}") private long expiration;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email, List<String> permissions) {
        return Jwts.builder()
            .setSubject(email)
            .claim("permissions", permissions)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getKey())
            .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        return (List<String>) getClaims(token).get("permissions");
    }

    public boolean validateToken(String token) {
        try { getClaims(token); return true; }
        catch (Exception e) { return false; }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
}