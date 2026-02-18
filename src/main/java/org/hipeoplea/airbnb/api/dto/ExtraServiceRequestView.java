package org.hipeoplea.airbnb.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hipeoplea.airbnb.model.ExtraServiceRequestStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExtraServiceRequestView {

    private UUID id;
    private ExtraServiceRequestStatus status;
    private String title;
    private String description;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private PaymentRequestView paymentRequest;
}
