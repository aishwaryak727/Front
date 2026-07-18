package com.teleconnect.analytics_service.client.mock;

import com.teleconnect.analytics_service.dto.external.FaultTicketDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MockFaultStatsClientTest {

    @Test
    void getClosedInPeriodParsesSpaceSeparatedDateTimes() {
        MockFaultStatsClient client = new MockFaultStatsClient();

        List<FaultTicketDto> tickets = client.getClosedInPeriod(
                LocalDateTime.of(2024, 6, 1, 0, 0),
                LocalDateTime.of(2024, 6, 30, 23, 59)
        );

        assertFalse(tickets.isEmpty());
        assertEquals(LocalDateTime.of(2024, 6, 1, 10, 0), tickets.get(0).getRaisedDate());
        assertEquals(LocalDateTime.of(2024, 6, 1, 14, 0), tickets.get(0).getResolvedDate());
    }
}
