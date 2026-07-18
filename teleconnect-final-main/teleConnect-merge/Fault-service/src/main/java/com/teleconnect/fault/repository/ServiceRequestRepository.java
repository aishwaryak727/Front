package com.teleconnect.fault.repository;

import com.teleconnect.fault.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Integer> {

    List<ServiceRequest> findByStatus(ServiceRequest.RequestStatus status);

    List<ServiceRequest> findByRequestType(ServiceRequest.RequestType requestType);

    List<ServiceRequest> findByAccountId(Integer accountId);
}
