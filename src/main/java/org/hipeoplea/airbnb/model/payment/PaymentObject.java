package org.hipeoplea.airbnb.model.payment;

import com.fasterxml.jackson.databind.JsonNode;

public record PaymentObject(String id, String status, Boolean paid, JsonNode rawPayload) {
}
