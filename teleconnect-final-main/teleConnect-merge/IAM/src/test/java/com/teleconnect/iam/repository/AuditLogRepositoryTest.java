package com.teleconnect.iam.repository;

import com.teleconnect.iam.entity.AuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogRepositoryTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Test
    void findById_returnsAuditLog() {
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditId(1L);
        auditLog.setAction("LOGIN");

        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(auditLog));

        Optional<AuditLog> result = auditLogRepository.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getAction()).isEqualTo("LOGIN");
    }
}
