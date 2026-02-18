package org.hipeoplea.airbnb.repository;

import java.util.List;
import java.util.UUID;
import org.hipeoplea.airbnb.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(UUID chatId);
}
