package org.hipeoplea.airbnb.repository;

import java.util.Optional;
import java.util.UUID;
import org.hipeoplea.airbnb.model.PaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {

    Optional<PaymentRequest> findByServiceRequestId(UUID serviceRequestId);
}
