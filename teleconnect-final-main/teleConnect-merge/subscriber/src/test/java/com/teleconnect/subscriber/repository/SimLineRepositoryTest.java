package com.teleconnect.subscriber.repository;

import com.teleconnect.subscriber.entity.SimLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimLineRepositoryTest {

    @Mock
    private SimLineRepository simLineRepository;

    @Test
    void findByMsisdn_returnsSimLine() {
        SimLine simLine = new SimLine();
        simLine.setLineId(5);
        simLine.setMsisdn("1234567890");

        when(simLineRepository.findByMsisdn("1234567890"))
                .thenReturn(Optional.of(simLine));

        Optional<SimLine> result = simLineRepository.findByMsisdn("1234567890");

        assertThat(result).isPresent();
        assertThat(result.get().getLineId()).isEqualTo(5);
    }
}
