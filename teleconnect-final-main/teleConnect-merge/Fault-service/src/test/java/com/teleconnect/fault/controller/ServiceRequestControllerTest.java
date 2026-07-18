package com.teleconnect.fault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teleconnect.fault.dto.request.ServiceRequestRequest;
import com.teleconnect.fault.dto.response.*;
import com.teleconnect.fault.service.ServiceRequestService;
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

@WebMvcTest(ServiceRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ServiceRequestControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper mapper;
    @MockBean private ServiceRequestService service;
    @MockBean private AuditClient auditClient;

    private ServiceRequestRequest validReq() {
        ServiceRequestRequest r = new ServiceRequestRequest();
        r.setAccountId(1); r.setLineId(1);
        r.setRequestType("PlanChange"); r.setRequestedBy(1);
        r.setRaisedDate(LocalDate.of(2026, 6, 1));
        return r;
    }

    private ServiceRequestResponse buildRes() {
        ServiceRequestResponse r = new ServiceRequestResponse();
        r.setRequestId(1); r.setAccountId(1); r.setLineId(1);
        r.setRequestType("PlanChange"); r.setRequestedBy(1);
        r.setRaisedDate(LocalDate.of(2026, 6, 1)); r.setStatus("O");
        return r;
    }

    // SR-C-01 : Positive — create request
    @Test
    void testCreateRequest_success() throws Exception {
        when(service.createRequest(any())).thenReturn(new MessageResponse("Service request created successfully"));
        mockMvc.perform(post("/fault/createRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(validReq())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Service request created successfully"));
    }

    // SR-C-02 : Positive — get all
    @Test
    void testGetAllRequests_success() throws Exception {
        when(service.getAllRequests()).thenReturn(List.of(buildRes()));
        mockMvc.perform(get("/fault/getAllRequests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value(1));
    }

    // SR-C-03 : Positive — get by ID
    @Test
    void testGetRequestById_success() throws Exception {
        when(service.getRequestById(1)).thenReturn(buildRes());
        mockMvc.perform(get("/fault/getRequests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(1));
    }

    // SR-C-04 : Positive — update request
    @Test
    void testUpdateRequest_success() throws Exception {
        when(service.updateRequest(eq(2), any())).thenReturn(new MessageResponse("Service request updated successfully"));
        ServiceRequestRequest req = new ServiceRequestRequest(); req.setStatus("P");
        mockMvc.perform(put("/fault/updateRequests/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Service request updated successfully"));
    }

    // SR-C-05 : Positive — cancel request
    @Test
    void testCancelRequest_success() throws Exception {
        when(service.cancelRequest(1)).thenReturn(new MessageResponse("Service request cancelled successfully"));
        mockMvc.perform(put("/fault/cancelRequests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Service request cancelled successfully"));
    }

    // SR-C-06 : Negative — create missing accountId
    @Test
    void testCreateRequest_missingAccountId() throws Exception {
        ServiceRequestRequest req = validReq(); req.setAccountId(null);
        mockMvc.perform(post("/fault/createRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // SR-C-07 : Negative — create invalid requestType
    @Test
    void testCreateRequest_invalidType() throws Exception {
        when(service.createRequest(any())).thenThrow(new RuntimeException(
                "requestType must be PlanChange, SIMReplacement, PortingRequest, or AccountUpdate"));
        ServiceRequestRequest req = validReq(); req.setRequestType("BillChange");
        mockMvc.perform(post("/fault/createRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("requestType must be PlanChange, SIMReplacement, PortingRequest, or AccountUpdate"));
    }

    // SR-C-08 : Negative — create missing raisedDate
    @Test
    void testCreateRequest_missingRaisedDate() throws Exception {
        ServiceRequestRequest req = validReq(); req.setRaisedDate(null);
        mockMvc.perform(post("/fault/createRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // SR-C-09 : Negative — get by ID not found
    @Test
    void testGetRequestById_notFound() throws Exception {
        when(service.getRequestById(999)).thenThrow(new RuntimeException("Service request with requestId 999 not found"));
        mockMvc.perform(get("/fault/getRequests/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Service request with requestId 999 not found"));
    }

    // SR-C-10 : Negative — update not found
    @Test
    void testUpdateRequest_notFound() throws Exception {
        when(service.updateRequest(eq(999), any())).thenThrow(new RuntimeException("Service request with requestId 999 not found"));
        ServiceRequestRequest req = new ServiceRequestRequest(); req.setStatus("P");
        mockMvc.perform(put("/fault/updateRequests/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // SR-C-11 : Negative — update invalid status
    @Test
    void testUpdateRequest_invalidStatus() throws Exception {
        when(service.updateRequest(eq(1), any())).thenThrow(new RuntimeException("status must be O, P, C, or X"));
        ServiceRequestRequest req = new ServiceRequestRequest(); req.setStatus("Z");
        mockMvc.perform(put("/fault/updateRequests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status must be O, P, C, or X"));
    }

    // SR-C-12 : Negative — cancel InProgress
    @Test
    void testCancelRequest_notOpen() throws Exception {
        when(service.cancelRequest(2)).thenThrow(new RuntimeException("Only Open requests can be cancelled"));
        mockMvc.perform(put("/fault/cancelRequests/2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only Open requests can be cancelled"));
    }

    // SR-C-13 : Negative — cancel not found
    @Test
    void testCancelRequest_notFound() throws Exception {
        when(service.cancelRequest(999)).thenThrow(new RuntimeException("Service request with requestId 999 not found"));
        mockMvc.perform(put("/fault/cancelRequests/999"))
                .andExpect(status().isNotFound());
    }

    // SR-C-14 : Boundary — get all on empty DB
    @Test
    void testGetAllRequests_empty() throws Exception {
        when(service.getAllRequests()).thenThrow(new RuntimeException("No service requests found"));
        mockMvc.perform(get("/fault/getAllRequests"))
                .andExpect(status().isBadRequest());
    }

    // SR-C-15 : Boundary — create with PortingRequest type
    @Test
    void testCreateRequest_portingType() throws Exception {
        when(service.createRequest(any())).thenReturn(new MessageResponse("Service request created successfully"));
        ServiceRequestRequest req = validReq(); req.setRequestType("PortingRequest");
        mockMvc.perform(post("/fault/createRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    // SR-C-16 : Boundary — update to Completed
    @Test
    void testUpdateRequest_completed() throws Exception {
        when(service.updateRequest(eq(1), any())).thenReturn(new MessageResponse("Service request updated successfully"));
        ServiceRequestRequest req = new ServiceRequestRequest(); req.setStatus("C");
        mockMvc.perform(put("/fault/updateRequests/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // SR-C-17 : Boundary — cancel Completed request
    @Test
    void testCancelRequest_completed() throws Exception {
        when(service.cancelRequest(4)).thenThrow(new RuntimeException("Only Open requests can be cancelled"));
        mockMvc.perform(put("/fault/cancelRequests/4"))
                .andExpect(status().isBadRequest());
    }

    // SR-C-18 : Boundary — get by ID zero
    @Test
    void testGetRequestById_zeroId() throws Exception {
        when(service.getRequestById(0)).thenThrow(new RuntimeException("Service request with requestId 0 not found"));
        mockMvc.perform(get("/fault/getRequests/0"))
                .andExpect(status().isNotFound());
    }
}
