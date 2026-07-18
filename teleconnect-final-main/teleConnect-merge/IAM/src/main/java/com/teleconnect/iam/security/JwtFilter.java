package com.teleconnect.iam.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtFilter extends OncePerRequestFilter {

    // This filter no longer validates JWTs. Authentication is performed by the API Gateway.
    // The gateway must forward authenticated user info in headers:
    // - X-Authenticated-User : user identifier (e.g. email)
    // - X-Authenticated-Permissions : comma-separated permissions/roles

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String user = req.getHeader("X-Authenticated-User");
        if (user != null && !user.isBlank()) {
            String permsHeader = req.getHeader("X-Authenticated-Permissions");
            List<SimpleGrantedAuthority> authorities = List.of();
            if (permsHeader != null && !permsHeader.isBlank()) {
                authorities = Arrays.stream(permsHeader.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());
            }

            var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(req, res);
    }
}
