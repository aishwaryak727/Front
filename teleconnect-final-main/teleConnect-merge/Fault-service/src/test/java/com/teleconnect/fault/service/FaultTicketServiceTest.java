package com.teleconnect.fault.service;

import com.teleconnect.fault.dto.request.FaultTicketRequest;
import com.teleconnect.fault.dto.response.*;
import com.teleconnect.fault.entity.FaultTicket;
import com.teleconnect.fault.repository.FaultTicketRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaultTicketServiceTest {

    @Mock private FaultTicketRepository repo;
    @InjectMocks private FaultTicketService service;

    private FaultTicketRequest buildReq(String faultType, String priority, String desc,
                                        LocalDate raisedDate) {
        FaultTicketRequest r = new FaultTicketRequest();
        r.setAccountId(1); r.setLineId(1);
        r.setFaultType(faultType); r.setDescription(desc);
        r.setPriority(priority); r.setRaisedDate(raisedDate);
        return r;
    }

    private FaultTicket buildEntity(Integer id, FaultTicket.TicketStatus status,
                                    LocalDate resolvedDate) {
        FaultTicket t = new FaultTicket();
        t.setTicketId(id); t.setAccountId(1); t.setLineId(1);
        t.setFaultType(FaultTicket.FaultType.SlowData);
        t.setDescription("Slow data");
        t.setPriority(FaultTicket.Priority.H);
        t.setRaisedDate(LocalDate.of(2026, 6, 1));
        t.setResolvedDate(resolvedDate);
        t.setStatus(status);
        return t;
    }

    // FT-S-01 : Positive — create ticket success
    @Test
    void testCreateTicket_success() {
        when(repo.save(any())).thenReturn(buildEntity(1, FaultTicket.TicketStatus.O, null));
        MessageResponse res = service.createTicket(
                buildReq("SlowData", "H", "Slow data since morning", LocalDate.of(2026, 6, 1)));
        assertEquals("Fault ticket created successfully", res.getMessage());
        verify(repo, times(1)).save(any());
    }

    // FT-S-02 : Positive — create ticket with no priority — defaults to M
    @Test
    void testCreateTicket_defaultPriority() {
        when(repo.save(any())).thenReturn(buildEntity(1, FaultTicket.TicketStatus.O, null));
        FaultTicketRequest req = buildReq("CallDrops", null, "Calls dropping", LocalDate.of(2026, 6, 1));
        MessageResponse res = service.createTicket(req);
        assertEquals("Fault ticket created successfully", res.getMessage());
    }

    // FT-S-03 : Positive — get all tickets
    @Test
    void testGetAllTickets_success() {
        when(repo.findAll()).thenReturn(List.of(buildEntity(1, FaultTicket.TicketStatus.O, null),
                buildEntity(2, FaultTicket.TicketStatus.P, null)));
        assertEquals(2, service.getAllTickets().size());
    }

    // FT-S-04 : Positive — get by ID
    @Test
    void testGetTicketById_success() {
        when(repo.findById(1)).thenReturn(Optional.of(buildEntity(1, FaultTicket.TicketStatus.O, null)));
        FaultTicketResponse res = service.getTicketById(1);
        assertEquals(1, res.getTicketId());
        assertNull(res.getResolvedDate());
    }

    // FT-S-05 : Positive — assign ticket
    @Test
    void testAssignTicket_success() {
        FaultTicket entity = buildEntity(1, FaultTicket.TicketStatus.O, null);
        when(repo.findById(1)).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenReturn(entity);
        FaultTicketRequest req = new FaultTicketRequest(); req.setAssignedToId(2);
        MessageResponse res = service.assignTicket(1, req);
        assertEquals("Fault ticket assigned successfully", res.getMessage());
    }

    // FT-S-06 : Positive — update status
    @Test
    void testUpdateTicket_success() {
        FaultTicket entity = buildEntity(1, FaultTicket.TicketStatus.O, null);
        when(repo.findById(1)).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenReturn(entity);
        FaultTicketRequest req = new FaultTicketRequest(); req.setStatus("P");
        assertEquals("Fault ticket updated successfully", service.updateTicket(1, req).getMessage());
    }

    // FT-S-07 : Negative — null description.
    // Description is required, but that rule is enforced by @NotBlank at the
    // controller/DTO layer (covered by FT-C-07), not by the service. At the
    // service layer the call therefore still completes — this test pins that
    // boundary so the validation responsibility is not silently moved.
    @Test
    void testCreateTicket_nullDescription() {
        when(repo.save(any())).thenReturn(buildEntity(1, FaultTicket.TicketStatus.O, null));
        FaultTicketRequest req = buildReq("SlowData", "H", null, LocalDate.of(2026, 6, 1));
        MessageResponse res = service.createTicket(req);
        assertEquals("Fault ticket created successfully", res.getMessage());
    }

    // FT-S-08 : Negative — create with invalid faultType
    @Test
    void testCreateTicket_invalidFaultType() {
        FaultTicketRequest req = buildReq("WeakSignal", "H", "Bad signal", LocalDate.of(2026, 6, 1));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createTicket(req));
        assertTrue(ex.getMessage().contains("faultType must be"));
    }

    // FT-S-09 : Negative — create with invalid priority
    @Test
    void testCreateTicket_invalidPriority() {
        FaultTicketRequest req = buildReq("SlowData", "X", "Slow data", LocalDate.of(2026, 6, 1));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createTicket(req));
        assertTrue(ex.getMessage().contains("priority must be"));
    }

    // FT-S-10 : Negative — getById not found
    @Test
    void testGetTicketById_notFound() {
        when(repo.findById(999)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getTicketById(999));
        assertTrue(ex.getMessage().contains("not found"));
    }

    // FT-S-11 : Negative — assign with null assignedToId
    @Test
    void testAssignTicket_nullAssignedToId() {
        when(repo.findById(1)).thenReturn(Optional.of(buildEntity(1, FaultTicket.TicketStatus.O, null)));
        FaultTicketRequest req = new FaultTicketRequest(); req.setAssignedToId(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.assignTicket(1, req));
        assertEquals("assignedToId is required", ex.getMessage());
    }

    // FT-S-12 : Negative — update invalid status
    @Test
    void testUpdateTicket_invalidStatus() {
        when(repo.findById(1)).thenReturn(Optional.of(buildEntity(1, FaultTicket.TicketStatus.O, null)));
        FaultTicketRequest req = new FaultTicketRequest(); req.setStatus("Z");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.updateTicket(1, req));
        assertTrue(ex.getMessage().contains("status must be"));
    }

    // FT-S-13 : Negative — resolve with null resolvedDate
    @Test
    void testResolveTicket_nullDate() {
        when(repo.findById(1)).thenReturn(Optional.of(buildEntity(1, FaultTicket.TicketStatus.P, null)));
        FaultTicketRequest req = new FaultTicketRequest(); req.setResolvedDate(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.resolveTicket(1, req));
        assertEquals("resolvedDate is required", ex.getMessage());
    }

    // FT-S-14 : Negative — resolve with date before raisedDate
    @Test
    void testResolveTicket_dateBeforeRaised() {
        when(repo.findById(1)).thenReturn(Optional.of(buildEntity(1, FaultTicket.TicketStatus.P, null)));
        FaultTicketRequest req = new FaultTicketRequest();
        req.setResolvedDate(LocalDate.of(2026, 5, 1));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.resolveTicket(1, req));
        assertEquals("resolvedDate cannot be before raisedDate", ex.getMessage());
    }

    // FT-S-15 : Negative — resolve ticket not found
    @Test
    void testResolveTicket_notFound() {
        when(repo.findById(999)).thenReturn(Optional.empty());
        FaultTicketRequest req = new FaultTicketRequest();
        req.setResolvedDate(LocalDate.of(2026, 6, 10));
        assertThrows(RuntimeException.class, () -> service.resolveTicket(999, req));
    }

    // FT-S-16 : Boundary — resolve with same date as raisedDate
    @Test
    void testResolveTicket_sameDayResolution() {
        FaultTicket entity = buildEntity(1, FaultTicket.TicketStatus.P, null);
        when(repo.findById(1)).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenReturn(entity);
        FaultTicketRequest req = new FaultTicketRequest();
        req.setResolvedDate(LocalDate.of(2026, 6, 1)); // same as raisedDate
        MessageResponse res = service.resolveTicket(1, req);
        assertEquals("Fault ticket resolved successfully", res.getMessage());
    }

    // FT-S-17 : Boundary — create with Activation faultType
    @Test
    void testCreateTicket_activationType() {
        when(repo.save(any())).thenReturn(buildEntity(1, FaultTicket.TicketStatus.O, null));
        MessageResponse res = service.createTicket(
                buildReq("Activation", "M", "SIM not activated", LocalDate.of(2026, 6, 1)));
        assertEquals("Fault ticket created successfully", res.getMessage());
    }

    // FT-S-18 : Boundary — update to Escalated status
    @Test
    void testUpdateTicket_escalated() {
        FaultTicket entity = buildEntity(1, FaultTicket.TicketStatus.O, null);
        when(repo.findById(1)).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenReturn(entity);
        FaultTicketRequest req = new FaultTicketRequest(); req.setStatus("E");
        assertEquals("Fault ticket updated successfully", service.updateTicket(1, req).getMessage());
    }
}
