package com.teleconnect.subscriber.repository;

import com.teleconnect.subscriber.entity.SubscriberAccount;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriberAccountRepositoryTest {

    @Mock
    private SubscriberAccountRepository repository;

    @Test
    void findByKycStatus_returnsList() {
        SubscriberAccount account = new SubscriberAccount();
        account.setAccountId(1);
        account.setKycStatus(SubscriberAccount.KycStatus.Expired);

        when(repository.findByKycStatus(SubscriberAccount.KycStatus.Expired))
                .thenReturn(List.of(account));

        List<SubscriberAccount> result = repository.findByKycStatus(SubscriberAccount.KycStatus.Expired);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKycStatus()).isEqualTo(SubscriberAccount.KycStatus.Expired);
    }
}
