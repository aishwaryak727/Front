package com.teleconnect.billing_service.repository;

import com.teleconnect.billing_service.entity.BillingDispute;
import com.teleconnect.billing_service.enums.DisputeStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingDisputeRepositoryTest {

    @Mock
    private BillingDisputeRepository billingDisputeRepository;

    @Test
    void findByStatus_returnsDisputes() {
        BillingDispute dispute = new BillingDispute();
        dispute.setDisputeId(1L);
        dispute.setStatus(DisputeStatus.OPEN);

        when(billingDisputeRepository.findByStatus(DisputeStatus.OPEN))
                .thenReturn(List.of(dispute));

        var result = billingDisputeRepository.findByStatus(DisputeStatus.OPEN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(DisputeStatus.OPEN);
    }
}
