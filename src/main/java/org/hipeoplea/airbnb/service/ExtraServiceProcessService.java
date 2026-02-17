package org.hipeoplea.airbnb.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hipeoplea.airbnb.api.dto.ChatMessageView;
import org.hipeoplea.airbnb.api.dto.CreateServiceRequestDto;
import org.hipeoplea.airbnb.api.dto.PaymentRequestView;
import org.hipeoplea.airbnb.api.dto.ProposeTermsDto;
import org.hipeoplea.airbnb.api.dto.ReceiptView;
import org.hipeoplea.airbnb.api.dto.ServiceRequestView;
import org.hipeoplea.airbnb.exceptions.BusinessException;
import org.hipeoplea.airbnb.exceptions.NotFoundException;
import org.hipeoplea.airbnb.model.ChatMessage;
import org.hipeoplea.airbnb.model.ChatSender;
import org.hipeoplea.airbnb.model.PaymentRequest;
import org.hipeoplea.airbnb.model.PaymentStatus;
import org.hipeoplea.airbnb.model.Receipt;
import org.hipeoplea.airbnb.model.RequestStatus;
import org.hipeoplea.airbnb.model.ServiceRequest;
import org.hipeoplea.airbnb.repository.ChatMessageRepository;
import org.hipeoplea.airbnb.repository.PaymentRequestRepository;
import org.hipeoplea.airbnb.repository.ReceiptRepository;
import org.hipeoplea.airbnb.repository.ServiceRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtraServiceProcessService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final ReceiptRepository receiptRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ExtraServiceProcessService(
            ServiceRequestRepository serviceRequestRepository,
            PaymentRequestRepository paymentRequestRepository,
            ReceiptRepository receiptRepository,
            ChatMessageRepository chatMessageRepository
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.paymentRequestRepository = paymentRequestRepository;
        this.receiptRepository = receiptRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public ServiceRequestView createRequest(CreateServiceRequestDto dto) {
        OffsetDateTime now = OffsetDateTime.now();
        ServiceRequest request = new ServiceRequest();
        request.setId(UUID.randomUUID());
        request.setGuestId(dto.getGuestId());
        request.setHostId(dto.getHostId());
        request.setGuestMessage(dto.getMessage());
        request.setCurrency("USD");
        request.setStatus(RequestStatus.INITIATED);
        request.setCreatedAt(now);
        request.setUpdatedAt(now);
        serviceRequestRepository.save(request);

        addChatMessage(request.getId(), ChatSender.GUEST, dto.getMessage(), now);
        addChatMessage(request.getId(), ChatSender.PLATFORM, "Airbnb доставил сообщение хосту.", now);

        return toServiceRequestView(request);
    }

    @Transactional
    public ServiceRequestView proposeTerms(UUID requestId, ProposeTermsDto dto) {
        ServiceRequest request = findServiceRequest(requestId);
        if (request.getStatus() != RequestStatus.INITIATED && request.getStatus() != RequestStatus.TERMS_PROPOSED) {
            throw new BusinessException("Нельзя согласовать условия в текущем статусе: " + request.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now();
        request.setHostTerms(dto.getTerms());
        request.setProposedAmount(normalizeAmount(dto.getAmount()));
        request.setCurrency(dto.getCurrency());
        request.setStatus(RequestStatus.TERMS_PROPOSED);
        request.setUpdatedAt(now);
        serviceRequestRepository.save(request);

        addChatMessage(requestId, ChatSender.HOST,
                "Готов оказать услугу. Цена: " + request.getProposedAmount() + " " + request.getCurrency()
                        + ". Условия: " + request.getHostTerms(), now);
        addChatMessage(requestId, ChatSender.PLATFORM, "Airbnb отправил цену и условия гостю.", now);

        return toServiceRequestView(request);
    }

    @Transactional
    public ServiceRequestView createPaymentRequest(UUID requestId) {
        ServiceRequest request = findServiceRequest(requestId);
        if (request.getStatus() != RequestStatus.TERMS_PROPOSED) {
            throw new BusinessException("Платежный запрос можно создать только после согласования условий.");
        }
        if (paymentRequestRepository.findByServiceRequestId(requestId).isPresent()) {
            throw new BusinessException("Платежный запрос уже существует.");
        }
        if (request.getProposedAmount() == null || request.getHostTerms() == null) {
            throw new BusinessException("Перед созданием платежа нужно зафиксировать цену и условия.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setId(UUID.randomUUID());
        paymentRequest.setServiceRequestId(requestId);
        paymentRequest.setAmount(request.getProposedAmount());
        paymentRequest.setCurrency(request.getCurrency());
        paymentRequest.setStatus(PaymentStatus.PENDING);
        paymentRequest.setCreatedAt(now);
        paymentRequestRepository.save(paymentRequest);

        request.setStatus(RequestStatus.PAYMENT_REQUESTED);
        request.setUpdatedAt(now);
        serviceRequestRepository.save(request);

        addChatMessage(requestId, ChatSender.HOST,
                "Создан платежный запрос в Resolution Center на " + paymentRequest.getAmount() + " "
                        + paymentRequest.getCurrency(), now);
        addChatMessage(requestId, ChatSender.PLATFORM,
                "Airbnb зарегистрировал платежный запрос и уведомил гостя.", now);

        return toServiceRequestView(request);
    }

    @Transactional
    public ServiceRequestView guestPaid(UUID paymentRequestId) {
        PaymentRequest paymentRequest = findPaymentRequest(paymentRequestId);
        if (paymentRequest.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Оплата невозможна, текущий статус платежа: " + paymentRequest.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now();
        paymentRequest.setStatus(PaymentStatus.PAID);
        paymentRequest.setResolvedAt(now);
        paymentRequestRepository.save(paymentRequest);

        ServiceRequest request = findServiceRequest(paymentRequest.getServiceRequestId());
        request.setStatus(RequestStatus.PAID);
        request.setUpdatedAt(now);
        serviceRequestRepository.save(request);

        Receipt receipt = new Receipt();
        receipt.setId(UUID.randomUUID());
        receipt.setPaymentRequestId(paymentRequest.getId());
        receipt.setReceiptNumber("RCP-" + now.toEpochSecond() + "-" + paymentRequest.getId().toString().substring(0, 8));
        receipt.setAmount(paymentRequest.getAmount());
        receipt.setCurrency(paymentRequest.getCurrency());
        receipt.setIssuedAt(now);
        receiptRepository.save(receipt);

        addChatMessage(request.getId(), ChatSender.PLATFORM,
                "Airbnb списал сумму, сформировал чек " + receipt.getReceiptNumber() + " и уведомил хоста.", now);
        addChatMessage(request.getId(), ChatSender.HOST, "Услуга оказана гостю.", now);

        return toServiceRequestView(request);
    }

    @Transactional
    public ServiceRequestView guestRejected(UUID paymentRequestId) {
        PaymentRequest paymentRequest = findPaymentRequest(paymentRequestId);
        if (paymentRequest.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Отклонение невозможно, текущий статус платежа: " + paymentRequest.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now();
        paymentRequest.setStatus(PaymentStatus.REJECTED);
        paymentRequest.setResolvedAt(now);
        paymentRequestRepository.save(paymentRequest);

        ServiceRequest request = findServiceRequest(paymentRequest.getServiceRequestId());
        request.setStatus(RequestStatus.REJECTED);
        request.setUpdatedAt(now);
        serviceRequestRepository.save(request);

        addChatMessage(request.getId(), ChatSender.PLATFORM,
                "Гость отклонил платеж. Запрос закрыт без оплаты.", now);

        return toServiceRequestView(request);
    }

    @Transactional
    public ServiceRequestView getRequest(UUID requestId) {
        return toServiceRequestView(findServiceRequest(requestId));
    }

    @Transactional(readOnly = true)
    public PaymentRequestView getPaymentRequest(UUID paymentRequestId) {
        return toPaymentRequestView(findPaymentRequest(paymentRequestId));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageView> getChat(UUID requestId) {
        findServiceRequest(requestId);
        return chatMessageRepository.findByServiceRequestIdOrderByCreatedAtAsc(requestId).stream()
                .map(this::toChatMessageView)
                .toList();
    }

    private ServiceRequest findServiceRequest(UUID requestId) {
        return serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Service request not found: " + requestId));
    }

    private PaymentRequest findPaymentRequest(UUID paymentRequestId) {
        return paymentRequestRepository.findById(paymentRequestId)
                .orElseThrow(() -> new NotFoundException("Payment request not found: " + paymentRequestId));
    }

    private void addChatMessage(UUID requestId, ChatSender sender, String message, OffsetDateTime at) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID());
        chatMessage.setServiceRequestId(requestId);
        chatMessage.setSender(sender);
        chatMessage.setMessage(message);
        chatMessage.setCreatedAt(at);
        chatMessageRepository.save(chatMessage);
    }

    private ServiceRequestView toServiceRequestView(ServiceRequest request) {
        PaymentRequestView paymentRequest = paymentRequestRepository.findByServiceRequestId(request.getId())
                .map(this::toPaymentRequestView)
                .orElse(null);

        List<ChatMessageView> chat = chatMessageRepository.findByServiceRequestIdOrderByCreatedAtAsc(request.getId())
                .stream()
                .map(this::toChatMessageView)
                .toList();

        return new ServiceRequestView(
                request.getId(),
                request.getGuestId(),
                request.getHostId(),
                request.getGuestMessage(),
                request.getHostTerms(),
                request.getProposedAmount(),
                request.getCurrency(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                paymentRequest,
                chat
        );
    }

    private PaymentRequestView toPaymentRequestView(PaymentRequest paymentRequest) {
        ReceiptView receipt = receiptRepository.findByPaymentRequestId(paymentRequest.getId())
                .map(this::toReceiptView)
                .orElse(null);

        return new PaymentRequestView(
                paymentRequest.getId(),
                paymentRequest.getServiceRequestId(),
                paymentRequest.getAmount(),
                paymentRequest.getCurrency(),
                paymentRequest.getStatus(),
                paymentRequest.getCreatedAt(),
                paymentRequest.getResolvedAt(),
                receipt
        );
    }

    private ReceiptView toReceiptView(Receipt receipt) {
        return new ReceiptView(
                receipt.getId(),
                receipt.getReceiptNumber(),
                receipt.getAmount(),
                receipt.getCurrency(),
                receipt.getIssuedAt()
        );
    }

    private ChatMessageView toChatMessageView(ChatMessage chatMessage) {
        return new ChatMessageView(
                chatMessage.getId(),
                chatMessage.getSender(),
                chatMessage.getMessage(),
                chatMessage.getCreatedAt()
        );
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
