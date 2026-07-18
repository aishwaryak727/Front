package com.teleconnect.fault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.fault.dto.request.FaultTicketRequest;
import com.teleconnect.fault.dto.response.*;
import com.teleconnect.fault.service.FaultTicketService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.teleconnect.common.audit.AuditClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FaultTicketController.class)
@AutoConfigureMockMvc(addFilters = false)
class FaultTicketControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @MockBean private FaultTicketService service;
    @MockBean private AuditClient auditClient;

    private FaultTicketRequest validReq() {
        FaultTicketRequest r = new FaultTicketRequest();
        r.setAccountId(1); r.setLineId(1);
        r.setFaultType("SlowData");
        r.setDescription("Mobile data very slow");
        r.setPriority("H");
        r.setRaisedDate(LocalDate.of(2026, 6, 1));
        return r;
    }

    private FaultTicketResponse buildRes(LocalDate resolvedDate, String status) {
        FaultTicketResponse r = new FaultTicketResponse();
        r.setTicketId(1); r.setAccountId(1); r.setLineId(1);
        r.setFaultType("SlowData"); r.setDescription("Mobile data very slow");
        r.setPriority("H"); r.setRaisedDate(LocalDate.of(2026, 6, 1));
        r.setResolvedDate(resolvedDate); r.setStatus(status);
        return r;
    }

    // FT-C-01 : Positive — create ticket with all fields
    @Test
    void testCreateTicket_success() throws Exception {
        when(service.createTicket(any())).thenReturn(new MessageResponse("Fault ticket created successfully"));
        mockMvc.perform(post("/fault/createTickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validReq())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Fault ticket created successfully"));
    }

    // FT-C-02 : Positive — create ticket without priority and assignedToId
    @Test
    void testCreateTicket_noOptionalFields() throws Exception {
        when(service.createTicket(any())).thenReturn(new MessageResponse("Fault ticket created successfully"));
        FaultTicketRequest req = validReq(); req.setPriority(null); req.setAssignedToId(null);
        mockMvc.perform(post("/fault/createTickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // FT-C-03 : Positive — get all tickets
    @Test
    void testGetAllTickets_success() throws Exception {
        when(service.getAllTickets()).thenReturn(List.of(buildRes(null, "O")));
        mockMvc.perform(get("/fault/getAllTickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketId").value(1))
                .andExpect(jsonPath("$[0].resolvedDate").doesNotExist());
    }

    // FT-C-04 : Positive — get resolved ticket by ID
    @Test
    void testGetTicketById_resolved() throws Exception {
        when(service.getTicketById(4)).thenReturn(buildRes(LocalDate.of(2026, 6, 6), "R"));
        mockMvc.perform(get("/fault/getTickets/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("R"))
                .andExpect(jsonPath("$.resolvedDate").value("2026-06-06"));
    }

    // FT-C-05 : Positive — assign ticket
    @Test
    void testAssignTicket_success() throws Exception {
        when(service.assignTicket(eq(1), any())).thenReturn(new MessageResponse("Fault ticket assigned successfully"));
        FaultTicketRequest req = new FaultTicketRequest(); req.setAssignedToId(2);
        mockMvc.perform(put("/fault/assignTickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Fault ticket assigned successfully"));
    }

    // FT-C-06 : Positive — resolve ticket
    @Test
    void testResolveTicket_success() throws Exception {
        when(service.resolveTicket(eq(2), any())).thenReturn(new MessageResponse("Fault ticket resolved successfully"));
        FaultTicketRequest req = new FaultTicketRequest(); req.setResolvedDate(LocalDate.of(2026, 6, 10));
        mockMvc.perform(put("/fault/resolveTickets/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Fault ticket resolved successfully"));
    }

    // FT-C-07 : Negative — create missing description
    @Test
    void testCreateTicket_missingDescription() throws Exception {
        FaultTicketRequest req = validReq(); req.setDescription(null);
        mockMvc.perform(post("/fault/createTickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // FT-C-08 : Negative — create invalid faultType
    @Test
    void testCreateTicket_invalidFaultType() throws Exception {
        when(service.createTicket(any())).thenThrow(new RuntimeException(
                "faultType must be NoCoverage, CallDrops, SlowData, BillingIssue, or Activation"));
        FaultTicketRequest req = validReq(); req.setFaultType("WeakSignal");
        mockMvc.perform(post("/fault/createTickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("faultType must be NoCoverage, CallDrops, SlowData, BillingIssue, or Activation"));
    }

    // FT-C-09 : Negative — create invalid priority
    @Test
    void testCreateTicket_invalidPriority() throws Exception {
        when(service.createTicket(any())).thenThrow(new RuntimeException("priority must be L, M, H, or C"));
        FaultTicketRequest req = validReq(); req.setPriority("X");
        mockMvc.perform(post("/fault/createTickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("priority must be L, M, H, or C"));
    }

    // FT-C-10 : Negative — create missing accountId
    @Test
    void testCreateTicket_missingAccountId() throws Exception {
        FaultTicketRequest req = validReq(); req.setAccountId(null);
        mockMvc.perform(post("/fault/createTickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // FT-C-11 : Negative — get by ID not found
    @Test
    void testGetTicketById_notFound() throws Exception {
        when(service.getTicketById(999)).thenThrow(new RuntimeException("Fault ticket with ticketId 999 not found"));
        mockMvc.perform(get("/fault/getTickets/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Fault ticket with ticketId 999 not found"));
    }

    // FT-C-12 : Negative — assign with null assignedToId
    @Test
    void testAssignTicket_nullId() throws Exception {
        when(service.assignTicket(eq(1), any())).thenThrow(new RuntimeException("assignedToId is required"));
        FaultTicketRequest req = new FaultTicketRequest(); req.setAssignedToId(null);
        mockMvc.perform(put("/fault/assignTickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("assignedToId is required"));
    }

    // FT-C-13 : Negative — assign ticket not found
    @Test
    void testAssignTicket_notFound() throws Exception {
        when(service.assignTicket(eq(999), any())).thenThrow(new RuntimeException("Fault ticket with ticketId 999 not found"));
        FaultTicketRequest req = new FaultTicketRequest(); req.setAssignedToId(1);
        mockMvc.perform(put("/fault/assignTickets/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // FT-C-14 : Negative — update invalid status
    @Test
    void testUpdateTicket_invalidStatus() throws Exception {
        when(service.updateTicket(eq(1), any())).thenThrow(new RuntimeException("status must be O, P, R, C, or E"));
        FaultTicketRequest req = new FaultTicketRequest(); req.setStatus("Z");
        mockMvc.perform(put("/fault/updateTickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status must be O, P, R, C, or E"));
    }

    // FT-C-15 : Negative — update not found
    @Test
    void testUpdateTicket_notFound() throws Exception {
        when(service.updateTicket(eq(999), any())).thenThrow(new RuntimeException("Fault ticket with ticketId 999 not found"));
        FaultTicketRequest req = new FaultTicketRequest(); req.setStatus("P");
        mockMvc.perform(put("/fault/updateTickets/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // FT-C-16 : Negative — resolve missing resolvedDate
    @Test
    void testResolveTicket_missingDate() throws Exception {
        when(service.resolveTicket(eq(1), any())).thenThrow(new RuntimeException("resolvedDate is required"));
        FaultTicketRequest req = new FaultTicketRequest();
        mockMvc.perform(put("/fault/resolveTickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("resolvedDate is required"));
    }

    // FT-C-17 : Negative — resolve date before raised date
    @Test
    void testResolveTicket_dateBeforeRaised() throws Exception {
        when(service.resolveTicket(eq(1), any())).thenThrow(new RuntimeException("resolvedDate cannot be before raisedDate"));
        FaultTicketRequest req = new FaultTicketRequest(); req.setResolvedDate(LocalDate.of(2026, 5, 1));
        mockMvc.perform(put("/fault/resolveTickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("resolvedDate cannot be before raisedDate"));
    }

    // FT-C-18 : Boundary — get all empty DB
    @Test
    void testGetAllTickets_empty() throws Exception {
        when(service.getAllTickets()).thenThrow(new RuntimeException("No fault tickets found"));
        mockMvc.perform(get("/fault/getAllTickets"))
                .andExpect(status().isBadRequest());
    }

    // FT-C-19 : Boundary — resolve same day as raisedDate
    @Test
    void testResolveTicket_sameDay() throws Exception {
        when(service.resolveTicket(eq(1), any())).thenReturn(new MessageResponse("Fault ticket resolved successfully"));
        FaultTicketRequest req = new FaultTicketRequest(); req.setResolvedDate(LocalDate.of(2026, 6, 1));
        mockMvc.perform(put("/fault/resolveTickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // FT-C-20 : Boundary — update to Escalated
    @Test
    void testUpdateTicket_escalated() throws Exception {
        when(service.updateTicket(eq(1), any())).thenReturn(new MessageResponse("Fault ticket updated successfully"));
        FaultTicketRequest req = new FaultTicketRequest(); req.setStatus("E");
        mockMvc.perform(put("/fault/updateTickets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // FT-C-21 : Boundary — get by ID zero
    @Test
    void testGetTicketById_zeroId() throws Exception {
        when(service.getTicketById(0)).thenThrow(new RuntimeException("Fault ticket with ticketId 0 not found"));
        mockMvc.perform(get("/fault/getTickets/0"))
                .andExpect(status().isNotFound());
    }

    // FT-C-22 : Boundary — create with Activation faultType
    @Test
    void testCreateTicket_activationType() throws Exception {
        when(service.createTicket(any())).thenReturn(new MessageResponse("Fault ticket created successfully"));
        FaultTicketRequest req = validReq(); req.setFaultType("Activation");
        mockMvc.perform(post("/fault/createTickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }
}
