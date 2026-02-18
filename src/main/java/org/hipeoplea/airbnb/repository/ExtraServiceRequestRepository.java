package org.hipeoplea.airbnb.repository;

import java.util.List;
import java.util.UUID;
import org.hipeoplea.airbnb.model.ExtraServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExtraServiceRequestRepository extends JpaRepository<ExtraServiceRequest, UUID> {

    List<ExtraServiceRequest> findByChatIdOrderByCreatedAtAsc(UUID chatId);
}
