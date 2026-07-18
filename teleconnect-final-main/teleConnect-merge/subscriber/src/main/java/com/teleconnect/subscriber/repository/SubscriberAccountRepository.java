package com.teleconnect.subscriber.repository;

import com.teleconnect.subscriber.entity.SubscriberAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriberAccountRepository
        extends JpaRepository<SubscriberAccount, Integer> {

    List<SubscriberAccount> findBySubscriberId(Long subscriberId);

    List<SubscriberAccount> findByStatus(SubscriberAccount.AccountStatus status);

    List<SubscriberAccount> findByKycStatus(SubscriberAccount.KycStatus kycStatus);

    boolean existsBySubscriberIdAndAccountType(
            Long subscriberId, SubscriberAccount.AccountType accountType);
}