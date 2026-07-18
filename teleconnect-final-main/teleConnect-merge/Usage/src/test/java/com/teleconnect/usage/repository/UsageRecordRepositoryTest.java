package com.teleconnect.usage.repository;

import com.teleconnect.usage.entity.UsageRecord;
import com.teleconnect.usage.entity.enums.UsageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsageRecordRepositoryTest {

    @Mock
    private UsageRecordRepository repository;

    @Test
    void findByLineId_returnsRecords() {
        UsageRecord record = new UsageRecord();
        record.setUsageId(1L);
        record.setLineId(42L);
        record.setUsageType(UsageType.DATA);
        record.setQuantity(BigDecimal.valueOf(100));

        when(repository.findByLineId(42L)).thenReturn(List.of(record));

        List<UsageRecord> result = repository.findByLineId(42L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLineId()).isEqualTo(42L);
    }
}
