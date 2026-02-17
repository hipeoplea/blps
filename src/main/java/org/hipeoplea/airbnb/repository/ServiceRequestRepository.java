package org.hipeoplea.airbnb.repository;

import java.util.UUID;
import org.hipeoplea.airbnb.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
}
