package org.hipeoplea.airbnb.service.payment;

import org.hipeoplea.airbnb.model.payment.PaymentObject;
import org.hipeoplea.airbnb.model.payment.WebhookNotification;
import org.hipeoplea.airbnb.service.ExtraServiceProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class YooKassaNotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(YooKassaNotificationProcessor.class);

    private final ExtraServiceProcessService processService;

    public YooKassaNotificationProcessor(ExtraServiceProcessService processService) {
        this.processService = processService;
    }

    public void process(WebhookNotification notification) {
        PaymentObject payment = notification.object();
        log.info("Received YooKassa event={} paymentId={} status={} paid={}",
                notification.event(),
                payment.id(),
                payment.status(),
                payment.paid());

        switch (notification.event()) {
            case "payment.succeeded" -> processService.processYooKassaPaymentSuccess(payment.id());
            case "payment.canceled" -> processService.processYooKassaPaymentFailure(payment.id());
            default -> log.warn("Unsupported YooKassa event: {}", notification.event());
        }
    }
}
