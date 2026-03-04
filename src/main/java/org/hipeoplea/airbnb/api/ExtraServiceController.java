package org.hipeoplea.airbnb.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.hipeoplea.airbnb.api.dto.CreateServiceRequestDto;
import org.hipeoplea.airbnb.api.dto.ExtraServiceFormDto;
import org.hipeoplea.airbnb.api.dto.ExtraServiceRequestView;
import org.hipeoplea.airbnb.api.dto.ServiceRequestView;
import org.hipeoplea.airbnb.api.dto.UpdateChatDto;
import org.hipeoplea.airbnb.api.dto.UpdateExtraServiceRequestDto;
import org.hipeoplea.airbnb.service.ExtraServiceProcessService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @GetMapping
    public List<ServiceRequestView> getChats() {
        return processService.getChats();
    }

    @GetMapping("/{chatId}")
    public ServiceRequestView getChat(@PathVariable UUID chatId) {
        return processService.getChat(chatId);
    }

    @PutMapping("/{chatId}")
    public ServiceRequestView updateChat(
            @PathVariable UUID chatId,
            @Valid @RequestBody UpdateChatDto dto
    ) {
        return processService.updateChat(chatId, dto);
    }

    @DeleteMapping("/{chatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteChat(@PathVariable UUID chatId) {
        processService.deleteChat(chatId);
    }

    @PostMapping("/{chatId}/extra-service/request")
    public ServiceRequestView createExtraServiceRequest(
            @PathVariable UUID chatId,
            @Valid @RequestBody ExtraServiceFormDto dto
    ) {
        return processService.createExtraServiceRequest(chatId, dto);
    }

    @GetMapping("/{chatId}/extra-service/requests")
    public List<ExtraServiceRequestView> getExtraServiceRequests(@PathVariable UUID chatId) {
        return processService.getExtraServiceRequests(chatId);
    }

    @GetMapping("/{chatId}/extra-service/requests/{requestId}")
    public ExtraServiceRequestView getExtraServiceRequest(
            @PathVariable UUID chatId,
            @PathVariable UUID requestId
    ) {
        return processService.getExtraServiceRequest(chatId, requestId);
    }

    @PutMapping("/{chatId}/extra-service/requests/{requestId}")
    public ExtraServiceRequestView updateExtraServiceRequest(
            @PathVariable UUID chatId,
            @PathVariable UUID requestId,
            @Valid @RequestBody UpdateExtraServiceRequestDto dto
    ) {
        return processService.updateExtraServiceRequest(chatId, requestId, dto);
    }

    @DeleteMapping("/{chatId}/extra-service/requests/{requestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExtraServiceRequest(
            @PathVariable UUID chatId,
            @PathVariable UUID requestId
    ) {
        processService.deleteExtraServiceRequest(chatId, requestId);
    }

    @PostMapping("/{chatId}/extra-service/requests/{requestId}/approve")
    public ServiceRequestView approveGuestPayment(
            @PathVariable UUID chatId,
            @PathVariable UUID requestId
    ) {
        return processService.approveGuestPayment(chatId, requestId);
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
