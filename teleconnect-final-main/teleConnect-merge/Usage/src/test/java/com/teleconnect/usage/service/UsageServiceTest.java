package com.teleconnect.usage.service;

import com.teleconnect.usage.dto.response.LimitStatusResponse;
import com.teleconnect.usage.entity.UsageSummary;
import com.teleconnect.usage.entity.enums.UsageUnit;
import com.teleconnect.usage.repository.UsageRecordRepository;
import com.teleconnect.usage.repository.UsageSummaryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

    @Mock
    private UsageRecordRepository recordRepo;

    @Mock
    private UsageSummaryRepository summaryRepo;

    @InjectMocks
    private UsageService service;

    @Test
    void getLimitStatus_returnsData() {
        UsageSummary summary = new UsageSummary();
        summary.setLineId(42L);
        summary.setBillingCycleId(99L);
        summary.setDataUsedMb(BigDecimal.valueOf(300));
        summary.setVoiceUsedMin(BigDecimal.valueOf(150));
        summary.setSmsUsed(20);

        when(summaryRepo.findByLineIdAndBillingCycleId(42L, 99L))
                .thenReturn(Optional.of(summary));

        LimitStatusResponse response = service.getLimitStatus(42L, 99L, 500, 200, 100);

        assertThat(response.getData().getStatus()).isEqualTo("WITHIN_LIMIT");
        assertThat(response.getVoice().getStatus()).isEqualTo("WITHIN_LIMIT");
    }
}
