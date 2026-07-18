package com.teleconnect.billing_service.repository;

import com.teleconnect.billing_service.entity.BillingCycle;
import com.teleconnect.billing_service.enums.BillingCycleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingCycleRepositoryTest {

    @Mock
    private BillingCycleRepository billingCycleRepository;

    @Test
    void findByStatus_returnsCycles() {
        BillingCycle cycle = new BillingCycle();
        cycle.setCycleId(1L);
        cycle.setStatus(BillingCycleStatus.OPEN);

        when(billingCycleRepository.findByStatus(BillingCycleStatus.OPEN))
                .thenReturn(List.of(cycle));

        var result = billingCycleRepository.findByStatus(BillingCycleStatus.OPEN);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BillingCycleStatus.OPEN);
    }
}
