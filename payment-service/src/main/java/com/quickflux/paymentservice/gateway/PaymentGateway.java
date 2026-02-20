package com.quickflux.paymentservice.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGateway {

    PreAuthResult preAuthorize(UUID orderId, BigDecimal amount, String idempotencyKey);
    CaptureResult capture(UUID preauthId, BigDecimal amount, String idempotencyKey);
    void voidPreAuth(UUID preauthId, String idempotencyKey);

    record PreAuthResult(UUID preauthId, String gatewayReferenceId, String status) {}
    record CaptureResult(String captureId, String status) {}
}