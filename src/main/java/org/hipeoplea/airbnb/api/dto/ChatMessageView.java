package org.hipeoplea.airbnb.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hipeoplea.airbnb.model.ChatSender;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageView {

    private UUID id;
    private ChatSender sender;
    private String message;
    private OffsetDateTime createdAt;
}
