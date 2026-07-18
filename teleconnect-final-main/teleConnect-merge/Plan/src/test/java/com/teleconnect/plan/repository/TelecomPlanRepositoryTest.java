package com.teleconnect.plan.repository;

import com.teleconnect.plan.entity.TelecomPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelecomPlanRepositoryTest {

    @Mock
    private TelecomPlanRepository repository;

    @Test
    void findById_returnsTelecomPlan() {
        TelecomPlan plan = new TelecomPlan();
        plan.setPlanId(1);
        plan.setName("Basic Plan");

        when(repository.findById(1)).thenReturn(Optional.of(plan));

        Optional<TelecomPlan> result = repository.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Basic Plan");
    }
}
