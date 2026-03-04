package org.hipeoplea.airbnb.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hipeoplea.airbnb.config.YooKassaProperties;
import org.hipeoplea.airbnb.exceptions.BusinessException;
import org.hipeoplea.airbnb.model.ExtraServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class YooKassaPaymentClient {

    private static final Logger log = LoggerFactory.getLogger(YooKassaPaymentClient.class);

    private final YooKassaProperties properties;
    private final RestClient restClient;

    public YooKassaPaymentClient(YooKassaProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl())
                .build();
    }

    public YooKassaCreatePaymentResponse createPayment(ExtraServiceRequest request) {
        validateConfiguration();

        String idempotenceKey = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "amount", Map.of(
                        "value", request.getAmount().toPlainString(),
                        "currency", request.getCurrency()
                ),
                "capture", true,
                "confirmation", Map.of(
                        "type", "redirect",
                        "return_url", properties.getReturnUrl()
                ),
                "description", "Extra service request " + request.getId(),
                "metadata", Map.of(
                        "extra_service_request_id", request.getId().toString(),
                        "chat_id", request.getChatId().toString()
                )
        );

        try {
            JsonNode response = restClient.post()
                    .uri("/v3/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotence-Key", idempotenceKey)
                    .headers(headers -> headers.setBasicAuth(properties.getShopId(), properties.getSecretKey()))
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new BusinessException("ЮКасса вернула пустой ответ при создании платежа.");
            }

            String paymentId = response.path("id").asText(null);
            String confirmationUrl = response.path("confirmation").path("confirmation_url").asText(null);
            String expiresAtRaw = response.path("expires_at").asText(null);

            if (!StringUtils.hasText(paymentId) || !StringUtils.hasText(confirmationUrl)) {
                throw new BusinessException("ЮКасса вернула неполный ответ: отсутствует id или confirmation_url.");
            }

            OffsetDateTime expiresAt = null;
            if (StringUtils.hasText(expiresAtRaw)) {
                expiresAt = OffsetDateTime.parse(expiresAtRaw);
            }

            log.info("YooKassa payment created: paymentId={} requestId={}", paymentId, request.getId());
            return new YooKassaCreatePaymentResponse(paymentId, confirmationUrl, expiresAt);
        } catch (RestClientException ex) {
            throw new BusinessException("Ошибка при вызове ЮКассы: " + ex.getMessage());
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.getShopId()) || !StringUtils.hasText(properties.getSecretKey())) {
            throw new BusinessException("Не заданы YOOKASSA_SHOP_ID или YOOKASSA_SECRET_KEY.");
        }
    }
}
