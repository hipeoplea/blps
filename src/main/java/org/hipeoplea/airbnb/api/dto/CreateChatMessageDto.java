package org.hipeoplea.airbnb.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hipeoplea.airbnb.model.ChatSender;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatMessageDto {

    @NotNull
    private ChatSender sender;

    @NotBlank
    private String message;
}
