package com.teleconnect.subscriber.service;

import com.teleconnect.subscriber.dto.request.UpdateAccountStatusRequest;
import com.teleconnect.subscriber.dto.request.UpdateKycRequest;
import com.teleconnect.subscriber.dto.response.MessageDTO;
import com.teleconnect.subscriber.entity.SimLine;
import com.teleconnect.subscriber.entity.SubscriberAccount;
import com.teleconnect.subscriber.repository.SimLineRepository;
import com.teleconnect.subscriber.repository.SubscriberAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriberAccountServiceTest {

    @Mock
    private SubscriberAccountRepository accountRepo;

    @Mock
    private SimLineRepository simLineRepo;

    @InjectMocks
    private SubscriberAccountService service;

    @Test
    void getExpiredKycAccounts_returnsResults() {
        SubscriberAccount account = new SubscriberAccount();
        account.setAccountId(1);
        account.setAccountType(SubscriberAccount.AccountType.Prepaid);
        account.setStatus(SubscriberAccount.AccountStatus.Active);
        account.setKycStatus(SubscriberAccount.KycStatus.Expired);

        when(accountRepo.findByKycStatus(SubscriberAccount.KycStatus.Expired))
                .thenReturn(List.of(account));

        var result = service.getExpiredKycAccounts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountId()).isEqualTo(1);
    }

    @Test
    void deleteAccount_terminatesActiveSimLines() {
        SubscriberAccount account = new SubscriberAccount();
        account.setAccountId(1);
        account.setStatus(SubscriberAccount.AccountStatus.Active); // active

        SimLine activeSim = new SimLine();
        activeSim.setAccountId(1);
        activeSim.setStatus(SimLine.SimStatus.Active);

        when(accountRepo.findById(1)).thenReturn(Optional.of(account));
        when(simLineRepo.findByAccountId(1)).thenReturn(List.of(activeSim));

        MessageDTO result = service.deleteAccount(1);

        assertThat(result.getMessage()).contains("terminated");
        assertThat(activeSim.getStatus()).isEqualTo(SimLine.SimStatus.Deactivated);
    }
}
