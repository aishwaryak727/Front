package com.teleconnect.plan.repository;

import com.teleconnect.plan.entity.ServiceSubscription;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceSubscriptionRepositoryTest {

    @Mock
    private ServiceSubscriptionRepository serviceSubscriptionRepository;

    @Test
    void findById_returnsSubscription() {
        ServiceSubscription subscription = new ServiceSubscription();
        subscription.setSubscriptionId(1);
        subscription.setPlanId(2);

        when(serviceSubscriptionRepository.findById(1)).thenReturn(Optional.of(subscription));

        Optional<ServiceSubscription> result = serviceSubscriptionRepository.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getPlanId()).isEqualTo(2);
    }
}
