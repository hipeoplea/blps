package org.hipeoplea.airbnb.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hipeoplea.airbnb.api.dto.ChatMessageView;
import org.hipeoplea.airbnb.api.dto.CreateChatMessageDto;
import org.hipeoplea.airbnb.api.dto.CreateServiceRequestDto;
import org.hipeoplea.airbnb.api.dto.ExtraServiceFormDto;
import org.hipeoplea.airbnb.api.dto.ExtraServiceRequestView;
import org.hipeoplea.airbnb.api.dto.PaymentRequestView;
import org.hipeoplea.airbnb.api.dto.ReceiptView;
import org.hipeoplea.airbnb.api.dto.ServiceRequestView;
import org.hipeoplea.airbnb.api.dto.UpdateChatDto;
import org.hipeoplea.airbnb.api.dto.UpdateExtraServiceRequestDto;
import org.hipeoplea.airbnb.exceptions.BusinessException;
import org.hipeoplea.airbnb.exceptions.NotFoundException;
import org.hipeoplea.airbnb.model.ChatMessage;
import org.hipeoplea.airbnb.model.ChatSender;
import org.hipeoplea.airbnb.model.ExtraServiceRequest;
import org.hipeoplea.airbnb.model.ExtraServiceRequestStatus;
import org.hipeoplea.airbnb.model.PaymentRequest;
import org.hipeoplea.airbnb.model.PaymentRequestStatus;
import org.hipeoplea.airbnb.model.Receipt;
import org.hipeoplea.airbnb.model.ServiceRequest;
import org.hipeoplea.airbnb.repository.ChatMessageRepository;
import org.hipeoplea.airbnb.repository.ExtraServiceRequestRepository;
import org.hipeoplea.airbnb.repository.PaymentRequestRepository;
import org.hipeoplea.airbnb.repository.ReceiptRepository;
import org.hipeoplea.airbnb.repository.ServiceRequestRepository;
import org.hipeoplea.airbnb.service.payment.YooKassaCreatePaymentResponse;
import org.hipeoplea.airbnb.service.payment.YooKassaPaymentClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtraServiceProcessService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ExtraServiceRequestRepository extraServiceRequestRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final ReceiptRepository receiptRepository;
    private final YooKassaPaymentClient yooKassaPaymentClient;
    private final ChatEventsPublisher chatEventsPublisher;

    public ExtraServiceProcessService(
            ServiceRequestRepository serviceRequestRepository,
            ChatMessageRepository chatMessageRepository,
            ExtraServiceRequestRepository extraServiceRequestRepository,
            PaymentRequestRepository paymentRequestRepository,
            ReceiptRepository receiptRepository,
            YooKassaPaymentClient yooKassaPaymentClient,
            ChatEventsPublisher chatEventsPublisher
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.extraServiceRequestRepository = extraServiceRequestRepository;
        this.paymentRequestRepository = paymentRequestRepository;
        this.receiptRepository = receiptRepository;
        this.yooKassaPaymentClient = yooKassaPaymentClient;
        this.chatEventsPublisher = chatEventsPublisher;
    }

    @Transactional
    public ServiceRequestView createChat(CreateServiceRequestDto dto) {
        OffsetDateTime now = OffsetDateTime.now();
        ServiceRequest chat = new ServiceRequest();
        chat.setId(UUID.randomUUID());
        chat.setGuestId(dto.getGuestId());
        chat.setHostId(dto.getHostId());
        chat.setCreatedAt(now);
        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);

        addChatMessage(chat.getId(), ChatSender.GUEST, dto.getMessage(), now);
        return toServiceRequestView(chat);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestView> getChats() {
        return serviceRequestRepository.findAll().stream()
                .map(this::toServiceRequestView)
                .toList();
    }

    @Transactional
    public ServiceRequestView updateChat(UUID chatId, UpdateChatDto dto) {
        ServiceRequest chat = findChat(chatId);
        OffsetDateTime now = OffsetDateTime.now();
        chat.setGuestId(dto.getGuestId());
        chat.setHostId(dto.getHostId());
        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);
        return toServiceRequestView(chat);
    }

    @Transactional
    public void deleteChat(UUID chatId) {
        ServiceRequest chat = findChat(chatId);
        List<ExtraServiceRequest> requests = extraServiceRequestRepository.findByChatIdOrderByCreatedAtAsc(chatId);
        for (ExtraServiceRequest request : requests) {
            deletePaymentArtifacts(request.getId());
            extraServiceRequestRepository.delete(request);
        }
        chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId)
                .forEach(chatMessageRepository::delete);
        serviceRequestRepository.delete(chat);
    }

    @Transactional
    public ChatMessageView postChatMessage(UUID chatId, CreateChatMessageDto dto) {
        ServiceRequest chat = findChat(chatId);
        if (dto.getSender() == ChatSender.PLATFORM) {
            throw new BusinessException("PLATFORM сообщения создаются только системой.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        ChatMessage message = addChatMessage(chatId, dto.getSender(), dto.getMessage(), now);
        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);
        return toChatMessageView(message);
    }

    @Transactional
    public ServiceRequestView createExtraServiceRequest(UUID chatId, ExtraServiceFormDto dto) {
        ServiceRequest chat = findChat(chatId);
        OffsetDateTime now = OffsetDateTime.now();

        ExtraServiceRequest extraServiceRequest = new ExtraServiceRequest();
        extraServiceRequest.setId(UUID.randomUUID());
        extraServiceRequest.setChatId(chatId);
        extraServiceRequest.setStatus(ExtraServiceRequestStatus.WAITING_GUEST_APPROVAL);
        extraServiceRequest.setTitle(dto.getTitle());
        extraServiceRequest.setDescription(dto.getDescription());
        extraServiceRequest.setAmount(normalizeAmount(dto.getAmount()));
        extraServiceRequest.setCurrency(dto.getCurrency());
        extraServiceRequest.setCreatedAt(now);
        extraServiceRequest.setUpdatedAt(now);
        extraServiceRequestRepository.save(extraServiceRequest);

        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);

        addChatMessage(chatId, ChatSender.HOST,
                "Создал заявку: " + dto.getTitle() + " / " + extraServiceRequest.getAmount() + " " + dto.getCurrency(),
                now);
        addChatMessage(chatId, ChatSender.PLATFORM,
                "Airbnb зарегистрировал запрос. Ожидаем подтверждение гостя для создания платежной ссылки.",
                now);

        return toServiceRequestView(chat);
    }

    @Transactional(readOnly = true)
    public List<ExtraServiceRequestView> getExtraServiceRequests(UUID chatId) {
        findChat(chatId);
        return extraServiceRequestRepository.findByChatIdOrderByCreatedAtAsc(chatId)
                .stream()
                .map(this::toExtraServiceRequestView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExtraServiceRequestView getExtraServiceRequest(UUID chatId, UUID requestId) {
        return toExtraServiceRequestView(findExtraServiceRequest(chatId, requestId));
    }

    @Transactional
    public ExtraServiceRequestView updateExtraServiceRequest(UUID chatId, UUID requestId, UpdateExtraServiceRequestDto dto) {
        ExtraServiceRequest request = findExtraServiceRequest(chatId, requestId);
        if (request.getStatus() != ExtraServiceRequestStatus.WAITING_GUEST_APPROVAL) {
            throw new BusinessException("Обновление заявки возможно только в статусе WAITING_GUEST_APPROVAL.");
        }

        request.setTitle(dto.getTitle());
        request.setDescription(dto.getDescription());
        request.setAmount(normalizeAmount(dto.getAmount()));
        request.setCurrency(dto.getCurrency());
        request.setUpdatedAt(OffsetDateTime.now());
        extraServiceRequestRepository.save(request);
        return toExtraServiceRequestView(request);
    }

    @Transactional
    public void deleteExtraServiceRequest(UUID chatId, UUID requestId) {
        ExtraServiceRequest request = findExtraServiceRequest(chatId, requestId);
        if (request.getStatus() == ExtraServiceRequestStatus.PAID
                || request.getStatus() == ExtraServiceRequestStatus.SERVICE_DELIVERED) {
            throw new BusinessException("Нельзя удалить заявку в статусе " + request.getStatus());
        }

        deletePaymentArtifacts(request.getId());
        extraServiceRequestRepository.delete(request);
    }

    @Transactional
    public ServiceRequestView approveGuestPayment(UUID chatId, UUID requestId) {
        ServiceRequest chat = findChat(chatId);
        ExtraServiceRequest request = findExtraServiceRequest(chatId, requestId);

        if (request.getStatus() != ExtraServiceRequestStatus.WAITING_GUEST_APPROVAL) {
            throw new BusinessException("Подтверждение оплаты возможно только для заявки со статусом WAITING_GUEST_APPROVAL.");
        }

        if (paymentRequestRepository.findByExtraServiceRequestId(request.getId()).isPresent()) {
            throw new BusinessException("Платежная ссылка уже создана для этой заявки.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        YooKassaCreatePaymentResponse paymentResponse = yooKassaPaymentClient.createPayment(request);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setId(UUID.randomUUID());
        paymentRequest.setExtraServiceRequestId(request.getId());
        paymentRequest.setProviderPaymentId(paymentResponse.paymentId());
        paymentRequest.setPaymentUrl(paymentResponse.paymentUrl());
        paymentRequest.setStatus(PaymentRequestStatus.WAITING_PAYMENT);
        paymentRequest.setCreatedAt(now);
        paymentRequest.setExpiresAt(paymentResponse.expiresAt());
        paymentRequestRepository.save(paymentRequest);

        request.setStatus(ExtraServiceRequestStatus.PAYMENT_LINK_SENT);
        request.setUpdatedAt(now);
        extraServiceRequestRepository.save(request);

        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);

        addChatMessage(chatId, ChatSender.PLATFORM,
                "Платежная ссылка ЮКассы создана и отправлена гостю: " + paymentResponse.paymentUrl(),
                now);

        return toServiceRequestView(chat);
    }

    @Transactional
    public ServiceRequestView rejectGuestPayment(UUID chatId, UUID requestId) {
        ServiceRequest chat = findChat(chatId);
        ExtraServiceRequest request = findExtraServiceRequest(chatId, requestId);

        OffsetDateTime now = OffsetDateTime.now();
        PaymentRequest paymentRequest = paymentRequestRepository.findByExtraServiceRequestId(request.getId()).orElse(null);
        if (paymentRequest != null && isFinalPaymentStatus(paymentRequest.getStatus())) {
            throw new BusinessException("Платеж уже обработан: " + paymentRequest.getStatus());
        }

        if (paymentRequest != null) {
            paymentRequest.setStatus(PaymentRequestStatus.REJECTED);
            paymentRequest.setResolvedAt(now);
            paymentRequestRepository.save(paymentRequest);
        }

        request.setStatus(ExtraServiceRequestStatus.REJECTED);
        request.setUpdatedAt(now);
        extraServiceRequestRepository.save(request);

        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);

        addChatMessage(chatId, ChatSender.PLATFORM, "Гость отказался от оплаты. Запрос закрыт.", now);
        return toServiceRequestView(chat);
    }

    @Transactional
    public void processYooKassaPaymentSuccess(String providerPaymentId) {
        PaymentRequest paymentRequest = findPaymentByProviderId(providerPaymentId);
        if (isFinalPaymentStatus(paymentRequest.getStatus())) {
            return;
        }

        ExtraServiceRequest request = findExtraServiceRequestById(paymentRequest.getExtraServiceRequestId());
        ServiceRequest chat = findChat(request.getChatId());

        OffsetDateTime now = OffsetDateTime.now();
        paymentRequest.setStatus(PaymentRequestStatus.PAID);
        paymentRequest.setResolvedAt(now);
        paymentRequestRepository.save(paymentRequest);

        request.setStatus(ExtraServiceRequestStatus.PAID);
        request.setUpdatedAt(now);
        extraServiceRequestRepository.save(request);

        Receipt receipt = receiptRepository.findByPaymentRequestId(paymentRequest.getId()).orElseGet(() -> {
            Receipt newReceipt = new Receipt();
            newReceipt.setId(UUID.randomUUID());
            newReceipt.setPaymentRequestId(paymentRequest.getId());
            newReceipt.setReceiptNumber(generateReceiptNumber(request.getId(), now));
            newReceipt.setAmount(request.getAmount());
            newReceipt.setCurrency(request.getCurrency());
            newReceipt.setIssuedAt(now);
            return receiptRepository.save(newReceipt);
        });

        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);

        addChatMessage(chat.getId(), ChatSender.PLATFORM,
                "Оплата подтверждена, сформирован чек " + receipt.getReceiptNumber() + ".",
                now);
        addChatMessage(chat.getId(), ChatSender.PLATFORM,
                "Хост уведомлен о поступлении оплаты.",
                now);
    }

    @Transactional
    public void processYooKassaPaymentFailure(String providerPaymentId) {
        PaymentRequest paymentRequest = findPaymentByProviderId(providerPaymentId);
        if (isFinalPaymentStatus(paymentRequest.getStatus())) {
            return;
        }

        ExtraServiceRequest request = findExtraServiceRequestById(paymentRequest.getExtraServiceRequestId());
        ServiceRequest chat = findChat(request.getChatId());

        OffsetDateTime now = OffsetDateTime.now();
        paymentRequest.setStatus(PaymentRequestStatus.FAILED);
        paymentRequest.setResolvedAt(now);
        paymentRequestRepository.save(paymentRequest);

        request.setStatus(ExtraServiceRequestStatus.PAYMENT_FAILED);
        request.setUpdatedAt(now);
        extraServiceRequestRepository.save(request);

        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);

        addChatMessage(chat.getId(), ChatSender.PLATFORM,
                "Списание неуспешно. Гость и хост уведомлены.",
                now);
    }

    @Transactional
    public ServiceRequestView markServiceDelivered(UUID chatId, UUID requestId) {
        ServiceRequest chat = findChat(chatId);
        ExtraServiceRequest request = findExtraServiceRequest(chatId, requestId);

        if (request.getStatus() != ExtraServiceRequestStatus.PAID) {
            throw new BusinessException("Оказать услугу можно только после успешной оплаты.");
        }

        OffsetDateTime now = OffsetDateTime.now();
        request.setStatus(ExtraServiceRequestStatus.SERVICE_DELIVERED);
        request.setUpdatedAt(now);
        extraServiceRequestRepository.save(request);

        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);

        addChatMessage(chatId, ChatSender.HOST, "Дополнительная услуга оказана.", now);
        return toServiceRequestView(chat);
    }

    @Transactional(readOnly = true)
    public ServiceRequestView getChat(UUID chatId) {
        return toServiceRequestView(findChat(chatId));
    }

    @Transactional(readOnly = true)
    public List<ChatMessageView> getChatMessages(UUID chatId) {
        findChat(chatId);
        return chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chatId)
                .stream()
                .map(this::toChatMessageView)
                .toList();
    }

    private ServiceRequest findChat(UUID chatId) {
        return serviceRequestRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("Chat not found: " + chatId));
    }

    private ExtraServiceRequest findExtraServiceRequest(UUID chatId, UUID requestId) {
        ExtraServiceRequest request = findExtraServiceRequestById(requestId);
        if (!request.getChatId().equals(chatId)) {
            throw new BusinessException("Заявка не принадлежит данному чату.");
        }
        return request;
    }

    private ExtraServiceRequest findExtraServiceRequestById(UUID requestId) {
        return extraServiceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Extra service request not found: " + requestId));
    }

    private PaymentRequest findPaymentByProviderId(String providerPaymentId) {
        return paymentRequestRepository.findByProviderPaymentId(providerPaymentId)
                .orElseThrow(() -> new NotFoundException("Payment request not found by provider payment id: " + providerPaymentId));
    }

    private ChatMessage addChatMessage(UUID chatId, ChatSender sender, String text, OffsetDateTime at) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID());
        chatMessage.setChatId(chatId);
        chatMessage.setSender(sender);
        chatMessage.setMessage(text);
        chatMessage.setCreatedAt(at);
        ChatMessage saved = chatMessageRepository.save(chatMessage);
        chatEventsPublisher.publishMessage(chatId, toChatMessageView(saved));
        return saved;
    }

    private void deletePaymentArtifacts(UUID extraServiceRequestId) {
        PaymentRequest paymentRequest = paymentRequestRepository.findByExtraServiceRequestId(extraServiceRequestId).orElse(null);
        if (paymentRequest == null) {
            return;
        }
        receiptRepository.deleteByPaymentRequestId(paymentRequest.getId());
        paymentRequestRepository.delete(paymentRequest);
    }

    private ServiceRequestView toServiceRequestView(ServiceRequest chat) {
        List<ChatMessageView> messages = chatMessageRepository.findByChatIdOrderByCreatedAtAsc(chat.getId())
                .stream()
                .map(this::toChatMessageView)
                .toList();

        List<ExtraServiceRequestView> requests = extraServiceRequestRepository.findByChatIdOrderByCreatedAtAsc(chat.getId())
                .stream()
                .map(this::toExtraServiceRequestView)
                .toList();

        return new ServiceRequestView(
                chat.getId(),
                chat.getGuestId(),
                chat.getHostId(),
                chat.getCreatedAt(),
                chat.getUpdatedAt(),
                requests,
                messages
        );
    }

    private ExtraServiceRequestView toExtraServiceRequestView(ExtraServiceRequest request) {
        PaymentRequestView paymentRequestView = paymentRequestRepository.findByExtraServiceRequestId(request.getId())
                .map(this::toPaymentRequestView)
                .orElse(null);

        return new ExtraServiceRequestView(
                request.getId(),
                request.getStatus(),
                request.getTitle(),
                request.getDescription(),
                request.getAmount(),
                request.getCurrency(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                paymentRequestView
        );
    }

    private PaymentRequestView toPaymentRequestView(PaymentRequest paymentRequest) {
        ReceiptView receiptView = receiptRepository.findByPaymentRequestId(paymentRequest.getId())
                .map(this::toReceiptView)
                .orElse(null);

        return new PaymentRequestView(
                paymentRequest.getId(),
                paymentRequest.getProviderPaymentId(),
                paymentRequest.getPaymentUrl(),
                paymentRequest.getStatus(),
                paymentRequest.getCreatedAt(),
                paymentRequest.getExpiresAt(),
                paymentRequest.getResolvedAt(),
                receiptView
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

    private ChatMessageView toChatMessageView(ChatMessage message) {
        return new ChatMessageView(
                message.getId(),
                message.getSender(),
                message.getMessage(),
                message.getCreatedAt()
        );
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String generateReceiptNumber(UUID requestId, OffsetDateTime now) {
        return "RCP-" + now.toEpochSecond() + "-" + requestId.toString().substring(0, 8);
    }

    private boolean isFinalPaymentStatus(PaymentRequestStatus status) {
        return status == PaymentRequestStatus.PAID
                || status == PaymentRequestStatus.FAILED
                || status == PaymentRequestStatus.REJECTED;
    }
}
