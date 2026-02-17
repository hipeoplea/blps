package org.hipeoplea.airbnb.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hipeoplea.airbnb.model.RequestStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestView {

    private UUID id;
    private String guestId;
    private String hostId;
    private String guestMessage;
    private String hostTerms;
    private BigDecimal proposedAmount;
    private String currency;
    private RequestStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private PaymentRequestView paymentRequest;
    private List<ChatMessageView> chat;
}
