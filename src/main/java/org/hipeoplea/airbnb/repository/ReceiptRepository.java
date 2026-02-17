package org.hipeoplea.airbnb.repository;

import java.util.Optional;
import java.util.UUID;
import org.hipeoplea.airbnb.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    Optional<Receipt> findByPaymentRequestId(UUID paymentRequestId);
}
