package com.teleconnect.common.audit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-registers the {@link AuditClient} bean in any module that puts
 * teleconnect-shared on its classpath. No component-scanning changes are
 * needed in the consuming module.
 *
 * <p>Override the target endpoint with the {@code teleconnect.audit.url} property.</p>
 */
@AutoConfiguration
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditClient auditClient(
            @Value("${teleconnect.audit.url:http://localhost:8081/teleConnect/iam/api/auditLogs}") String auditUrl) {
        return new AuditClient(auditUrl);
    }
}
