package com.quickflux.paymentservice.service;

import com.quickflux.paymentservice.domain.Payment;
import com.quickflux.paymentservice.domain.PaymentStatus;
import com.quickflux.paymentservice.gateway.PaymentGateway;
import com.quickflux.paymentservice.gateway.PaymentGateway.CaptureResult;
import com.quickflux.paymentservice.gateway.PaymentGateway.PreAuthResult;
import com.quickflux.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentGateway paymentGateway;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PreAuthResult preAuthorizePayment(UUID orderId, BigDecimal amount) {
        String idempotencyKey = "preauth-" + orderId.toString();

        log.info("Pre-authorizing payment for order {}: ${}", orderId, amount);

        try {
            PreAuthResult result = paymentGateway.preAuthorize(orderId, amount, idempotencyKey);

            // Save payment record
            Payment payment = Payment.builder()
                    .id(UUID.randomUUID())
                    .orderId(orderId)
                    .amount(amount)
                    .status(PaymentStatus.PRE_AUTHORIZED)
                    .preauthId(result.preauthId())
                    .gatewayReferenceId(result.gatewayReferenceId())
                    .preauthAt(Instant.now())
                    .preauthIdempotencyKey(idempotencyKey)
                    .build();

            paymentRepository.save(payment);
            log.info("Pre-auth successful and saved: {}", result);

            return result;

        } catch (Exception e) {
            log.error("Pre-auth failed for order {}: {}", orderId, e.getMessage());
            throw new PaymentPreAuthFailedException("Payment pre-authorization failed", e);
        }
    }

    @Transactional
    public CaptureResult capturePayment(UUID preauthId, BigDecimal amount) {
        String idempotencyKey = "capture-" + preauthId.toString();

        log.info("Capturing payment for pre-auth {}: ${}", preauthId, amount);

        Payment payment = paymentRepository.findByPreauthId(preauthId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for preauth: " + preauthId));

        try {
            CaptureResult result = paymentGateway.capture(preauthId, amount, idempotencyKey);

            payment.markAsCaptured(result.captureId());
            paymentRepository.save(payment);

            log.info("Capture successful: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Capture failed for pre-auth {}: {}", preauthId, e.getMessage());
            payment.markAsFailed();
            paymentRepository.save(payment);
            throw new PaymentCaptureFailedException("Payment capture failed", e);
        }
    }
}