package com.quickflux.orderservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String PAYMENT_SERVICE_URL = "http://localhost:8082";

    public PreAuthResult preAuthorizePayment(UUID orderId, BigDecimal amount) {
        String url = PAYMENT_SERVICE_URL + "/api/payments/preauth?orderId=" + orderId + "&amount=" + amount;
        log.info("Calling Payment Service: {}", url);

        return restTemplate.postForObject(url, null, PreAuthResult.class);
    }

    public record PreAuthResult(UUID preauthId, String gatewayReferenceId, String status) {}
}