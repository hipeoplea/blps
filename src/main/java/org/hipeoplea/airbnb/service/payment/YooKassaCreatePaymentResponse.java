package org.hipeoplea.airbnb.service.payment;

import java.time.OffsetDateTime;

public record YooKassaCreatePaymentResponse(String paymentId, String paymentUrl, OffsetDateTime expiresAt) {
}
