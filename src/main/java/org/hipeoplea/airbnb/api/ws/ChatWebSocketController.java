package org.hipeoplea.airbnb.api.ws;

import jakarta.validation.Valid;
import java.util.UUID;
import org.hipeoplea.airbnb.api.dto.CreateChatMessageDto;
import org.hipeoplea.airbnb.service.ExtraServiceProcessService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

@Controller
public class ChatWebSocketController {

    private final ExtraServiceProcessService processService;

    public ChatWebSocketController(ExtraServiceProcessService processService) {
        this.processService = processService;
    }

    @MessageMapping("/chats/{chatId}/messages")
    public void postMessage(
            @DestinationVariable UUID chatId,
            @Valid @Payload CreateChatMessageDto dto
    ) {
        processService.postChatMessage(chatId, dto);
    }
}
