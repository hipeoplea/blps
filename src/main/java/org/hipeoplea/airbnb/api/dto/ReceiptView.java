package org.hipeoplea.airbnb.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptView {

    private UUID id;
    private String receiptNumber;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime issuedAt;
}
