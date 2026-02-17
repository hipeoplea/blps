package org.hipeoplea.airbnb.api;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.hipeoplea.airbnb.api.dto.ChatMessageView;
import org.hipeoplea.airbnb.api.dto.CreateServiceRequestDto;
import org.hipeoplea.airbnb.api.dto.PaymentRequestView;
import org.hipeoplea.airbnb.api.dto.ProposeTermsDto;
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
@RequestMapping("/api")
public class ExtraServiceController {

    private final ExtraServiceProcessService processService;

    public ExtraServiceController(ExtraServiceProcessService processService) {
        this.processService = processService;
    }

    @PostMapping("/service-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceRequestView createServiceRequest(@Valid @RequestBody CreateServiceRequestDto dto) {
        return processService.createRequest(dto);
    }

    @PostMapping("/service-requests/{requestId}/terms")
    public ServiceRequestView proposeTerms(
            @PathVariable UUID requestId,
            @Valid @RequestBody ProposeTermsDto dto
    ) {
        return processService.proposeTerms(requestId, dto);
    }

    @PostMapping("/service-requests/{requestId}/payment-requests")
    public ServiceRequestView createPaymentRequest(@PathVariable UUID requestId) {
        return processService.createPaymentRequest(requestId);
    }

    @PostMapping("/payment-requests/{paymentRequestId}/pay")
    public ServiceRequestView guestPay(@PathVariable UUID paymentRequestId) {
        return processService.guestPaid(paymentRequestId);
    }

    @PostMapping("/payment-requests/{paymentRequestId}/reject")
    public ServiceRequestView guestReject(@PathVariable UUID paymentRequestId) {
        return processService.guestRejected(paymentRequestId);
    }

    @GetMapping("/service-requests/{requestId}")
    public ServiceRequestView getServiceRequest(@PathVariable UUID requestId) {
        return processService.getRequest(requestId);
    }

    @GetMapping("/payment-requests/{paymentRequestId}")
    public PaymentRequestView getPaymentRequest(@PathVariable UUID paymentRequestId) {
        return processService.getPaymentRequest(paymentRequestId);
    }

    @GetMapping("/service-requests/{requestId}/chat")
    public List<ChatMessageView> getChat(@PathVariable UUID requestId) {
        return processService.getChat(requestId);
    }
}
