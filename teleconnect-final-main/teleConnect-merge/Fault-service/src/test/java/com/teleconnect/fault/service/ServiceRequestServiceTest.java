package com.teleconnect.fault.service;

import com.teleconnect.fault.dto.request.ServiceRequestRequest;
import com.teleconnect.fault.dto.response.*;
import com.teleconnect.fault.entity.ServiceRequest;
import com.teleconnect.fault.repository.ServiceRequestRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock private ServiceRequestRepository repo;
    @InjectMocks private ServiceRequestService service;

    private ServiceRequestRequest buildReq(String type) {
        ServiceRequestRequest r = new ServiceRequestRequest();
        r.setAccountId(1); r.setLineId(1);
        r.setRequestType(type); r.setRequestedBy(1);
        r.setRaisedDate(LocalDate.of(2026, 6, 1));
        return r;
    }

    private ServiceRequest buildEntity(Integer id, ServiceRequest.RequestStatus status) {
        ServiceRequest sr = new ServiceRequest();
        sr.setRequestId(id); sr.setAccountId(1); sr.setLineId(1);
        sr.setRequestType(ServiceRequest.RequestType.PlanChange);
        sr.setRequestedBy(1); sr.setRaisedDate(LocalDate.of(2026, 6, 1));
        sr.setStatus(status);
        return sr;
    }

    // SR-S-01 : Positive — create request success
    @Test
    void testCreateRequest_success() {
        when(repo.save(any())).thenReturn(buildEntity(1, ServiceRequest.RequestStatus.O));
        MessageResponse res = service.createRequest(buildReq("PlanChange"));
        assertEquals("Service request created successfully", res.getMessage());
        verify(repo, times(1)).save(any());
    }

    // SR-S-02 : Positive — get all requests
    @Test
    void testGetAllRequests_success() {
        when(repo.findAll()).thenReturn(List.of(buildEntity(1, ServiceRequest.RequestStatus.O),
                buildEntity(2, ServiceRequest.RequestStatus.P)));
        List<ServiceRequestResponse> list = service.getAllRequests();
        assertEquals(2, list.size());
    }

    // SR-S-03 : Positive — get by ID success
    @Test
    void testGetRequestById_success() {
        when(repo.findById(1)).thenReturn(Optional.of(buildEntity(1, ServiceRequest.RequestStatus.O)));
        ServiceRequestResponse res = service.getRequestById(1);
        assertEquals(1, res.getRequestId());
    }

    // SR-S-04 : Positive — update status success
    @Test
    void testUpdateRequest_success() {
        ServiceRequest entity = buildEntity(1, ServiceRequest.RequestStatus.O);
        when(repo.findById(1)).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenReturn(entity);
        ServiceRequestRequest req = new ServiceRequestRequest();
        req.setStatus("P");
        MessageResponse res = service.updateRequest(1, req);
        assertEquals("Service request updated successfully", res.getMessage());
    }

    // SR-S-05 : Positive — cancel open request
    @Test
    void testCancelRequest_success() {
        ServiceRequest entity = buildEntity(1, ServiceRequest.RequestStatus.O);
        when(repo.findById(1)).thenReturn(Optional.of(entity));
        when(repo.save(any())).thenReturn(entity);
        MessageResponse res = service.cancelRequest(1);
        assertEquals("Service request cancelled successfully", res.getMessage());
    }

    // SR-S-06 : Negative — create with null requestType
    @Test
    void testCreateRequest_nullType() {
        ServiceRequestRequest req = buildReq(null);
        assertThrows(NullPointerException.class, () -> service.createRequest(req));
    }

    // SR-S-07 : Negative — create with invalid requestType
    @Test
    void testCreateRequest_invalidType() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createRequest(buildReq("BillChange")));
        assertTrue(ex.getMessage().contains("requestType must be"));
    }

    // SR-S-08 : Negative — getById not found
    @Test
    void testGetRequestById_notFound() {
        when(repo.findById(999)).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getRequestById(999));
        assertTrue(ex.getMessage().contains("not found"));
    }

    // SR-S-09 : Negative — update not found
    @Test
    void testUpdateRequest_notFound() {
        when(repo.findById(999)).thenReturn(Optional.empty());
        ServiceRequestRequest req = new ServiceRequestRequest(); req.setStatus("P");
        assertThrows(RuntimeException.class, () -> service.updateRequest(999, req));
    }

    // SR-S-10 : Negative — update invalid status
    @Test
    void testUpdateRequest_invalidStatus() {
        when(repo.findById(1)).thenReturn(Optional.of(buildEntity(1, ServiceRequest.RequestStatus.O)));
        ServiceRequestRequest req = new ServiceRequestRequest(); req.setStatus("Z");
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateRequest(1, req));
        assertTrue(ex.getMessage().contains("status must be"));
    }

    // SR-S-11 : Negative — cancel InProgress request
    @Test
    void testCancelRequest_notOpen() {
        when(repo.findById(2)).thenReturn(Optional.of(buildEntity(2, ServiceRequest.RequestStatus.P)));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cancelRequest(2));
        assertEquals("Only Open requests can be cancelled", ex.getMessage());
    }

    // SR-S-12 : Negative — cancel not found
    @Test
    void testCancelRequest_notFound() {
        when(repo.findById(999)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.cancelRequest(999));
    }

    // SR-S-13 : Boundary — create with AccountUpdate type
    @Test
    void testCreateRequest_accountUpdateType() {
        when(repo.save(any())).thenReturn(buildEntity(1, ServiceRequest.RequestStatus.O));
        MessageResponse res = service.createRequest(buildReq("AccountUpdate"));
        assertEquals("Service request created successfully", res.getMessage());
    }

    // SR-S-14 : Boundary — cancel Completed request
    @Test
    void testCancelRequest_completed() {
        when(repo.findById(4)).thenReturn(Optional.of(buildEntity(4, ServiceRequest.RequestStatus.C)));
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.cancelRequest(4));
        assertEquals("Only Open requests can be cancelled", ex.getMessage());
    }
}
