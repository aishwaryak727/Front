package com.teleconnect.fault.repository;

import com.teleconnect.fault.entity.ServiceRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ServiceRequestRepositoryTest {

    @Autowired
    private ServiceRequestRepository repo;

    private ServiceRequest buildRequest(ServiceRequest.RequestType type,
                                        ServiceRequest.RequestStatus status) {
        ServiceRequest sr = new ServiceRequest();
        sr.setAccountId(1);
        sr.setLineId(1);
        sr.setRequestType(type);
        sr.setRequestedBy(1);
        sr.setRaisedDate(LocalDate.of(2026, 6, 1));
        sr.setStatus(status);
        return sr;
    }

    // SR-R-01 : Positive — save valid request
    @Test
    void testSaveRequest_success() {
        ServiceRequest sr = buildRequest(ServiceRequest.RequestType.PlanChange,
                ServiceRequest.RequestStatus.O);
        ServiceRequest saved = repo.save(sr);
        assertNotNull(saved.getRequestId());
        assertEquals(ServiceRequest.RequestType.PlanChange, saved.getRequestType());
    }

    // SR-R-02 : Positive — findById existing
    @Test
    void testFindById_exists() {
        ServiceRequest saved =
                repo.save(buildRequest(ServiceRequest.RequestType.SIMReplacement,
                        ServiceRequest.RequestStatus.O));
        Optional<ServiceRequest> result = repo.findById(saved.getRequestId());
        assertTrue(result.isPresent());
        assertEquals(ServiceRequest.RequestType.SIMReplacement,
                result.get().getRequestType());
    }

    // SR-R-03 : Positive — findAll
    @Test
    void testFindAll_returnsList() {
        repo.save(buildRequest(ServiceRequest.RequestType.PlanChange,
                ServiceRequest.RequestStatus.O));
        repo.save(buildRequest(ServiceRequest.RequestType.PortingRequest,
                ServiceRequest.RequestStatus.P));
        List<ServiceRequest> list = repo.findAll();
        assertEquals(2, list.size());
    }

    // SR-R-04 : Positive — findByStatus
    @Test
    void testFindByStatus_open() {
        repo.save(buildRequest(ServiceRequest.RequestType.PlanChange,
                ServiceRequest.RequestStatus.O));
        repo.save(buildRequest(ServiceRequest.RequestType.AccountUpdate,
                ServiceRequest.RequestStatus.C));
        List<ServiceRequest> result = repo.findByStatus(ServiceRequest.RequestStatus.O);
        assertEquals(1, result.size());
        assertEquals(ServiceRequest.RequestStatus.O, result.get(0).getStatus());
    }

    // SR-R-05 : Negative — findById non-existing
    @Test
    void testFindById_notFound() {
        Optional<ServiceRequest> result = repo.findById(999);
        assertTrue(result.isEmpty());
    }

    // SR-R-06 : Negative — findByStatus returns empty
    @Test
    void testFindByStatus_noMatch() {
        repo.save(buildRequest(ServiceRequest.RequestType.PlanChange,
                ServiceRequest.RequestStatus.O));
        List<ServiceRequest> result = repo.findByStatus(ServiceRequest.RequestStatus.X);
        assertTrue(result.isEmpty());
    }

    // SR-R-07 : Negative — findByRequestType returns empty
    @Test
    void testFindByRequestType_noMatch() {
        repo.save(buildRequest(ServiceRequest.RequestType.PlanChange,
                ServiceRequest.RequestStatus.O));
        List<ServiceRequest> result =
                repo.findByRequestType(ServiceRequest.RequestType.PortingRequest);
        assertTrue(result.isEmpty());
    }

    // SR-R-08 : Boundary — save with minimum required fields
    @Test
    void testSave_minimumFields() {
        ServiceRequest sr = new ServiceRequest();
        sr.setAccountId(1);
        sr.setLineId(1);
        sr.setRequestType(ServiceRequest.RequestType.AccountUpdate);
        sr.setRequestedBy(1);
        sr.setRaisedDate(LocalDate.now());
        ServiceRequest saved = repo.save(sr);
        assertNotNull(saved.getRequestId());
        assertEquals(ServiceRequest.RequestStatus.O, saved.getStatus());
    }
}
