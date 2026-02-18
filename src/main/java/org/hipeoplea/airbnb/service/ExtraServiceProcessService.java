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
import org.hipeoplea.airbnb.api.dto.PaymentProcessResult;
import org.hipeoplea.airbnb.api.dto.PaymentRequestView;
import org.hipeoplea.airbnb.api.dto.ProcessPaymentDto;
import org.hipeoplea.airbnb.api.dto.ReceiptView;
import org.hipeoplea.airbnb.api.dto.ServiceRequestView;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExtraServiceProcessService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ExtraServiceRequestRepository extraServiceRequestRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final ReceiptRepository receiptRepository;

    public ExtraServiceProcessService(
            ServiceRequestRepository serviceRequestRepository,
            ChatMessageRepository chatMessageRepository,
            ExtraServiceRequestRepository extraServiceRequestRepository,
            PaymentRequestRepository paymentRequestRepository,
            ReceiptRepository receiptRepository
    ) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.extraServiceRequestRepository = extraServiceRequestRepository;
        this.paymentRequestRepository = paymentRequestRepository;
        this.receiptRepository = receiptRepository;
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
        extraServiceRequest.setStatus(ExtraServiceRequestStatus.REQUEST_CREATED);
        extraServiceRequest.setTitle(dto.getTitle());
        extraServiceRequest.setDescription(dto.getDescription());
        extraServiceRequest.setAmount(normalizeAmount(dto.getAmount()));
        extraServiceRequest.setCurrency(dto.getCurrency());
        extraServiceRequest.setCreatedAt(now);
        extraServiceRequest.setUpdatedAt(now);
        extraServiceRequestRepository.save(extraServiceRequest);

        PaymentRequest paymentRequest = new PaymentRequest();
        paymentRequest.setId(UUID.randomUUID());
        paymentRequest.setExtraServiceRequestId(extraServiceRequest.getId());
        paymentRequest.setStatus(PaymentRequestStatus.PENDING);
        paymentRequest.setCreatedAt(now);
        paymentRequestRepository.save(paymentRequest);

        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);

        addChatMessage(chatId, ChatSender.HOST,
                "Создал заявку: " + dto.getTitle() + " / " + extraServiceRequest.getAmount() + " " + dto.getCurrency(),
                now);
        addChatMessage(chatId, ChatSender.PLATFORM,
                "Airbnb зарегистрировал запрос и отправил гостю уведомление об оплате.", now);

        return toServiceRequestView(chat);
    }

    @Transactional
    public ServiceRequestView processGuestPayment(UUID chatId, UUID requestId, ProcessPaymentDto dto) {
        ServiceRequest chat = findChat(chatId);
        ExtraServiceRequest request = findExtraServiceRequest(chatId, requestId);
        PaymentRequest paymentRequest = findPaymentRequest(request.getId());

        if (paymentRequest.getStatus() != PaymentRequestStatus.PENDING) {
            throw new BusinessException("Платеж уже обработан: " + paymentRequest.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (dto.getResult() == PaymentProcessResult.SUCCESS) {
            paymentRequest.setStatus(PaymentRequestStatus.PAID);
            paymentRequest.setResolvedAt(now);
            paymentRequestRepository.save(paymentRequest);

            request.setStatus(ExtraServiceRequestStatus.PAID);
            request.setUpdatedAt(now);
            extraServiceRequestRepository.save(request);

            Receipt receipt = new Receipt();
            receipt.setId(UUID.randomUUID());
            receipt.setPaymentRequestId(paymentRequest.getId());
            receipt.setReceiptNumber(generateReceiptNumber(request.getId(), now));
            receipt.setAmount(request.getAmount());
            receipt.setCurrency(request.getCurrency());
            receipt.setIssuedAt(now);
            receiptRepository.save(receipt);

            addChatMessage(chatId, ChatSender.PLATFORM,
                    "Оплата подтверждена, сформирован чек " + receipt.getReceiptNumber() + ".", now);
            addChatMessage(chatId, ChatSender.PLATFORM, "Хост уведомлен о поступлении оплаты.", now);
        } else {
            paymentRequest.setStatus(PaymentRequestStatus.FAILED);
            paymentRequest.setResolvedAt(now);
            paymentRequestRepository.save(paymentRequest);

            request.setStatus(ExtraServiceRequestStatus.PAYMENT_FAILED);
            request.setUpdatedAt(now);
            extraServiceRequestRepository.save(request);

            addChatMessage(chatId, ChatSender.PLATFORM,
                    "Списание неуспешно. Гость и хост уведомлены.", now);
        }

        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);
        return toServiceRequestView(chat);
    }

    @Transactional
    public ServiceRequestView rejectGuestPayment(UUID chatId, UUID requestId) {
        ServiceRequest chat = findChat(chatId);
        ExtraServiceRequest request = findExtraServiceRequest(chatId, requestId);
        PaymentRequest paymentRequest = findPaymentRequest(request.getId());

        if (paymentRequest.getStatus() != PaymentRequestStatus.PENDING) {
            throw new BusinessException("Платеж уже обработан: " + paymentRequest.getStatus());
        }

        OffsetDateTime now = OffsetDateTime.now();
        paymentRequest.setStatus(PaymentRequestStatus.REJECTED);
        paymentRequest.setResolvedAt(now);
        paymentRequestRepository.save(paymentRequest);

        request.setStatus(ExtraServiceRequestStatus.REJECTED);
        request.setUpdatedAt(now);
        extraServiceRequestRepository.save(request);

        chat.setUpdatedAt(now);
        serviceRequestRepository.save(chat);

        addChatMessage(chatId, ChatSender.PLATFORM, "Гость отказался от оплаты. Запрос закрыт.", now);
        return toServiceRequestView(chat);
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
        ExtraServiceRequest request = extraServiceRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Extra service request not found: " + requestId));
        if (!request.getChatId().equals(chatId)) {
            throw new BusinessException("Заявка не принадлежит данному чату.");
        }
        return request;
    }

    private PaymentRequest findPaymentRequest(UUID extraServiceRequestId) {
        return paymentRequestRepository.findByExtraServiceRequestId(extraServiceRequestId)
                .orElseThrow(() -> new NotFoundException("Payment request not found for extra service request: " + extraServiceRequestId));
    }

    private ChatMessage addChatMessage(UUID chatId, ChatSender sender, String text, OffsetDateTime at) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID());
        chatMessage.setChatId(chatId);
        chatMessage.setSender(sender);
        chatMessage.setMessage(text);
        chatMessage.setCreatedAt(at);
        return chatMessageRepository.save(chatMessage);
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
                paymentRequest.getStatus(),
                paymentRequest.getCreatedAt(),
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
}
