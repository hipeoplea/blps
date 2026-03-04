package org.hipeoplea.airbnb.model.payment;

import com.fasterxml.jackson.databind.JsonNode;

public record WebhookNotification(String event, PaymentObject object) {

    public static WebhookNotification fromJson(JsonNode root) {
        if (root == null || root.isNull()) {
            throw new IllegalArgumentException("Request body is empty");
        }

        JsonNode eventNode = root.get("event");
        JsonNode objectNode = root.get("object");
        if (eventNode == null || eventNode.isNull() || objectNode == null || objectNode.isNull()) {
            throw new IllegalArgumentException("Required fields 'event' and 'object' are missing");
        }

        String event = eventNode.asText();
        String id = objectNode.path("id").asText(null);
        String status = objectNode.path("status").asText(null);
        Boolean paid = objectNode.has("paid") ? objectNode.path("paid").asBoolean() : null;

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Payment id is missing in notification object");
        }

        return new WebhookNotification(event, new PaymentObject(id, status, paid, objectNode));
    }
}
