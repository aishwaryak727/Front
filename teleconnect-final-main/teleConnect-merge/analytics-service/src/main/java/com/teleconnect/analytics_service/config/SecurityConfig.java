package com.teleconnect.analytics_service.config;

import com.teleconnect.analytics_service.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/teleConnect/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/teleConnect/swagger-ui/**",
                    "/swagger-ui.html",
                    "/teleConnect/swagger-ui.html",
                    "/webjars/**"
                ).permitAll()
                .requestMatchers("/api/reports/arpu/**").hasAuthority("VIEW_REPORT_ARPU")
                .requestMatchers("/api/reports/churn/**").hasAuthority("VIEW_REPORT_CHURN")
                .requestMatchers("/api/reports/network-utilisation/**").hasAuthority("VIEW_REPORT_NETWORK_UTILISATION")
                .requestMatchers("/api/reports/sla-compliance/**").hasAuthority("VIEW_REPORT_SLA_COMPLIANCE")
                .requestMatchers("/api/reports/collection-efficiency/**").hasAuthority("VIEW_REPORT_COLLECTION_EFFICIENCY")
                .requestMatchers("/api/reports/subscriber-growth/**").hasAuthority("VIEW_REPORT_SUBSCRIBER_GROWTH")
                .requestMatchers("/api/reports/generate").hasAuthority("GENERATE_REPORT")
                .requestMatchers("/api/reports/dashboard/export").authenticated()
                .requestMatchers("/api/reports/*/export").authenticated()
                .requestMatchers("/api/reports/**").authenticated()
                .requestMatchers("/api/dashboard/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
