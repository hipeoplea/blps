package org.hipeoplea.airbnb.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hipeoplea.airbnb.model.PaymentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestView {

    private UUID id;
    private UUID serviceRequestId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
    private ReceiptView receipt;
}
