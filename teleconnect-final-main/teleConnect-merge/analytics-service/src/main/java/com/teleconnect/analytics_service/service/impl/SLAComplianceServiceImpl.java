package com.teleconnect.analytics_service.service.impl;

import com.teleconnect.analytics_service.client.FaultStatsClient;
import com.teleconnect.analytics_service.dto.external.FaultTicketDto;
import com.teleconnect.analytics_service.dto.response.SLAComplianceResponse;
import com.teleconnect.analytics_service.enums.FaultPriority;
import com.teleconnect.analytics_service.service.SLAComplianceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes SLA compliance entirely from fault tickets fetched live
 * from the Fault & Service Request Management module's analytics API.
 * The breach-escalation job also delegates the actual status update
 * back to that module via FaultStatsClient.escalateTicket(...), since
 * analytics-service does not own the fault_tickets table.
 */
@Slf4j
@Service
public class SLAComplianceServiceImpl implements SLAComplianceService {

    private final FaultStatsClient faultStatsClient;

    public SLAComplianceServiceImpl(FaultStatsClient faultStatsClient) {
        this.faultStatsClient = faultStatsClient;
    }

    @Override
    public SLAComplianceResponse computeSLACompliance(LocalDate periodStart, LocalDate periodEnd) {
        List<FaultTicketDto> closedTickets = faultStatsClient.getClosedInPeriod(
                periodStart.atStartOfDay(), periodEnd.plusDays(1).atStartOfDay());

        Map<FaultPriority, long[]> stats = new EnumMap<>(FaultPriority.class);
        Map<FaultPriority, double[]> resolutionHours = new EnumMap<>(FaultPriority.class);

        for (FaultPriority p : FaultPriority.values()) {
            stats.put(p, new long[]{0, 0, 0}); // [total, met, breached]
            resolutionHours.put(p, new double[]{0.0, 0});
        }

        for (FaultTicketDto ticket : closedTickets) {
            FaultPriority priority = ticket.getPriority();
            long[] s = stats.get(priority);
            double[] rh = resolutionHours.get(priority);
            s[0]++;

            if (ticket.getResolvedDate() != null) {
                double hours = Duration.between(ticket.getRaisedDate(), ticket.getResolvedDate())
                        .toMinutes() / 60.0;
                rh[0] += hours;
                rh[1]++;

                if (hours <= priority.getSlaHours()) {
                    s[1]++;
                } else {
                    s[2]++;
                }
            } else {
                s[2]++;
            }
        }

        long totalClosed = closedTickets.size();
        long totalMet = stats.values().stream().mapToLong(s -> s[1]).sum();
        long totalBreached = stats.values().stream().mapToLong(s -> s[2]).sum();
        double overallRate = totalClosed > 0 ? (double) totalMet / totalClosed * 100 : 0.0;
        double avgHours = closedTickets.stream()
                .filter(t -> t.getResolvedDate() != null)
                .mapToDouble(t -> Duration.between(t.getRaisedDate(), t.getResolvedDate()).toMinutes() / 60.0)
                .average().orElse(0.0);

        long escalatedCount = faultStatsClient.countEscalated();

        Map<String, SLAComplianceResponse.PriorityStats> priorityStatsMap = new HashMap<>();
        for (FaultPriority priority : FaultPriority.values()) {
            long[] s = stats.get(priority);
            double[] rh = resolutionHours.get(priority);
            SLAComplianceResponse.PriorityStats ps = new SLAComplianceResponse.PriorityStats();
            ps.setTotal(s[0]);
            ps.setSlaMet(s[1]);
            ps.setSlaBreached(s[2]);
            ps.setComplianceRate(s[0] > 0 ? (double) s[1] / s[0] * 100 : 0.0);
            ps.setAvgResolutionHours(rh[1] > 0 ? rh[0] / rh[1] : 0.0);
            priorityStatsMap.put(priority.name(), ps);
        }

        SLAComplianceResponse response = new SLAComplianceResponse();
        response.setPeriodStart(periodStart);
        response.setPeriodEnd(periodEnd);
        response.setTotalTicketsClosed(totalClosed);
        response.setTotalBreaches(totalBreached);
        response.setEscalatedCount(escalatedCount);
        response.setOverallComplianceRate(Math.round(overallRate * 100.0) / 100.0);
        response.setAvgResolutionHours(Math.round(avgHours * 100.0) / 100.0);
        response.setStatsByPriority(priorityStatsMap);

        return response;
    }

    @Override
    public void escalateBreachingTickets() {
        LocalDateTime now = LocalDateTime.now();

        for (FaultPriority priority : FaultPriority.values()) {
            LocalDateTime slaDeadline = now.minusHours(priority.getSlaHours());
            List<FaultTicketDto> breaching = faultStatsClient.getOpenBreachingBefore(slaDeadline)
                    .stream()
                    .filter(t -> t.getPriority() == priority)
                    .toList();

            for (FaultTicketDto ticket : breaching) {
                faultStatsClient.escalateTicket(ticket.getTicketId());
                log.warn("Requested escalation of ticket {} (Priority: {}) - breached SLA of {}h",
                        ticket.getTicketId(), priority, priority.getSlaHours());
            }
        }
    }
}
