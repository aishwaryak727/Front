package com.teleconnect.fault.repository;

import com.teleconnect.fault.entity.FaultTicket;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class FaultTicketRepositoryTest {

    @Autowired
    private FaultTicketRepository repo;

    private FaultTicket buildTicket(FaultTicket.FaultType type, FaultTicket.Priority priority,
                                    FaultTicket.TicketStatus status, Integer assignedToId) {
        FaultTicket t = new FaultTicket();
        t.setAccountId(1);
        t.setLineId(1);
        t.setFaultType(type);
        t.setDescription("Test fault description");
        t.setPriority(priority);
        t.setRaisedDate(LocalDate.of(2026, 6, 1));
        t.setAssignedToId(assignedToId);
        t.setStatus(status);
        return t;
    }

    // FT-R-01 : Positive — save valid ticket
    @Test
    void testSaveTicket_success() {
        FaultTicket saved = repo.save(buildTicket(FaultTicket.FaultType.SlowData,
                FaultTicket.Priority.H, FaultTicket.TicketStatus.O, 1));
        assertNotNull(saved.getTicketId());
        assertEquals(FaultTicket.FaultType.SlowData, saved.getFaultType());
    }

    // FT-R-02 : Positive — findById existing
    @Test
    void testFindById_exists() {
        FaultTicket saved = repo.save(buildTicket(FaultTicket.FaultType.CallDrops,
                FaultTicket.Priority.H, FaultTicket.TicketStatus.O, 1));
        Optional<FaultTicket> result = repo.findById(saved.getTicketId());
        assertTrue(result.isPresent());
        assertEquals(FaultTicket.FaultType.CallDrops, result.get().getFaultType());
    }

    // FT-R-03 : Positive — findAll
    @Test
    void testFindAll_returnsList() {
        repo.save(buildTicket(FaultTicket.FaultType.SlowData, FaultTicket.Priority.H,
                FaultTicket.TicketStatus.O, 1));
        repo.save(buildTicket(FaultTicket.FaultType.NoCoverage, FaultTicket.Priority.C,
                FaultTicket.TicketStatus.E, 2));
        assertEquals(2, repo.findAll().size());
    }

    // FT-R-04 : Positive — findByStatus
    @Test
    void testFindByStatus_open() {
        repo.save(buildTicket(FaultTicket.FaultType.SlowData, FaultTicket.Priority.H,
                FaultTicket.TicketStatus.O, 1));
        repo.save(buildTicket(FaultTicket.FaultType.CallDrops, FaultTicket.Priority.H,
                FaultTicket.TicketStatus.R, 2));
        List<FaultTicket> result = repo.findByStatus(FaultTicket.TicketStatus.O);
        assertEquals(1, result.size());
    }

    // FT-R-05 : Positive — findByPriority
    @Test
    void testFindByPriority_high() {
        repo.save(buildTicket(FaultTicket.FaultType.SlowData, FaultTicket.Priority.H,
                FaultTicket.TicketStatus.O, 1));
        repo.save(buildTicket(FaultTicket.FaultType.NoCoverage, FaultTicket.Priority.M,
                FaultTicket.TicketStatus.O, 1));
        List<FaultTicket> result = repo.findByPriority(FaultTicket.Priority.H);
        assertEquals(1, result.size());
    }

    // FT-R-06 : Negative — findById non-existing
    @Test
    void testFindById_notFound() {
        assertTrue(repo.findById(999).isEmpty());
    }

    // FT-R-07 : Negative — findByFaultType no match
    @Test
    void testFindByFaultType_noMatch() {
        repo.save(buildTicket(FaultTicket.FaultType.SlowData, FaultTicket.Priority.H,
                FaultTicket.TicketStatus.O, 1));
        assertTrue(repo.findByFaultType(FaultTicket.FaultType.Activation).isEmpty());
    }

    // FT-R-08 : Negative — findByAssignedToId no match
    @Test
    void testFindByAssignedToId_noMatch() {
        assertTrue(repo.findByAssignedToId(99).isEmpty());
    }

    // FT-R-09 : Boundary — save with null assignedToId and resolvedDate
    @Test
    void testSave_nullableFields() {
        FaultTicket saved = repo.save(buildTicket(FaultTicket.FaultType.BillingIssue,
                FaultTicket.Priority.M, FaultTicket.TicketStatus.O, null));
        assertNotNull(saved.getTicketId());
        assertNull(saved.getAssignedToId());
        assertNull(saved.getResolvedDate());
    }

    // FT-R-10 : Boundary — save without setting priority — should default to M
    @Test
    void testSave_defaultPriority() {
        FaultTicket t = new FaultTicket();
        t.setAccountId(1);
        t.setLineId(1);
        t.setFaultType(FaultTicket.FaultType.Activation);
        t.setDescription("SIM not activated");
        t.setRaisedDate(LocalDate.now());
        FaultTicket saved = repo.save(t);
        assertEquals(FaultTicket.Priority.M, saved.getPriority());
    }
}
