package org.hipeoplea.airbnb.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.hipeoplea.airbnb.api.dto.ChatMessageView;
import org.hipeoplea.airbnb.api.dto.CreateChatMessageDto;
import org.hipeoplea.airbnb.api.dto.CreateServiceRequestDto;
import org.hipeoplea.airbnb.api.dto.ExtraServiceFormDto;
import org.hipeoplea.airbnb.api.dto.ProcessPaymentDto;
import org.hipeoplea.airbnb.api.dto.ServiceRequestView;
import org.hipeoplea.airbnb.service.ExtraServiceProcessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
public class ExtraServiceController {

    private final ExtraServiceProcessService processService;

    public ExtraServiceController(ExtraServiceProcessService processService) {
        this.processService = processService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestView createChat(@Valid @RequestBody CreateServiceRequestDto dto) {
        return processService.createChat(dto);
    }

    @GetMapping("/{chatId}")
    public ServiceRequestView getChat(@PathVariable UUID chatId) {
        return processService.getChat(chatId);
    }

    @GetMapping("/{chatId}/messages")
    public List<ChatMessageView> getChatMessages(@PathVariable UUID chatId) {
        return processService.getChatMessages(chatId);
    }

    @PostMapping("/{chatId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageView postChatMessage(
            @PathVariable UUID chatId,
            @Valid @RequestBody CreateChatMessageDto dto
    ) {
        return processService.postChatMessage(chatId, dto);
    }

    @PostMapping("/{chatId}/extra-service/request")
    public ServiceRequestView createExtraServiceRequest(
            @PathVariable UUID chatId,
            @Valid @RequestBody ExtraServiceFormDto dto
    ) {
        return processService.createExtraServiceRequest(chatId, dto);
    }

    @PostMapping("/{chatId}/extra-service/requests/{requestId}/pay")
    public ServiceRequestView processGuestPayment(
            @PathVariable UUID chatId,
            @PathVariable UUID requestId,
            @Valid @RequestBody ProcessPaymentDto dto
    ) {
        return processService.processGuestPayment(chatId, requestId, dto);
    }

    @PostMapping("/{chatId}/extra-service/requests/{requestId}/reject")
    public ServiceRequestView rejectGuestPayment(
            @PathVariable UUID chatId,
            @PathVariable UUID requestId
    ) {
        return processService.rejectGuestPayment(chatId, requestId);
    }

    @PostMapping("/{chatId}/extra-service/requests/{requestId}/deliver")
    public ServiceRequestView markServiceDelivered(
            @PathVariable UUID chatId,
            @PathVariable UUID requestId
    ) {
        return processService.markServiceDelivered(chatId, requestId);
    }
}
