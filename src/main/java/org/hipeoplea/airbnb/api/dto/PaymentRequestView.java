package org.hipeoplea.airbnb.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hipeoplea.airbnb.model.PaymentRequestStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestView {

    private UUID id;
    private PaymentRequestStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime resolvedAt;
    private ReceiptView receipt;
}
