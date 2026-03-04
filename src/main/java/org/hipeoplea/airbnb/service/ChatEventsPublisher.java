package org.hipeoplea.airbnb.service;

import java.util.UUID;
import org.hipeoplea.airbnb.api.dto.ChatMessageView;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class ChatEventsPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatEventsPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishMessage(UUID chatId, ChatMessageView message) {
        messagingTemplate.convertAndSend("/topic/chats/" + chatId + "/messages", message);
    }
}
