package org.hipeoplea.airbnb.api.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hipeoplea.airbnb.model.payment.WebhookNotification;
import org.hipeoplea.airbnb.service.payment.YooKassaNotificationProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/yookassa")
public class YooKassaWebhookController {

    private static final Logger log = LoggerFactory.getLogger(YooKassaWebhookController.class);

    private final ObjectMapper objectMapper;
    private final YooKassaNotificationProcessor notificationProcessor;

    public YooKassaWebhookController(ObjectMapper objectMapper, YooKassaNotificationProcessor notificationProcessor) {
        this.objectMapper = objectMapper;
        this.notificationProcessor = notificationProcessor;
    }

    @PostMapping("/notification")
    public ResponseEntity<Void> handleWebhook(@RequestBody String requestBody) {
        try {
            JsonNode root = objectMapper.readTree(requestBody);
            WebhookNotification notification = WebhookNotification.fromJson(root);
            notificationProcessor.process(notification);
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Invalid YooKassa webhook payload", ex);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }
}
