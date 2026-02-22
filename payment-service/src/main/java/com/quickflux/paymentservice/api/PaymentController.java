package com.quickflux.paymentservice.api;

import com.quickflux.paymentservice.gateway.PaymentGateway.PreAuthResult;
import com.quickflux.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/preauth")
    public ResponseEntity<PreAuthResult> preAuthorize(
            @RequestParam UUID orderId,
            @RequestParam BigDecimal amount) {

        log.info("Received pre-auth request for order {}, amount: {}", orderId, amount);

        PreAuthResult result = paymentService.preAuthorizePayment(orderId, amount);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/void")
    public ResponseEntity<Void> voidPreAuth(@RequestParam UUID preauthId) {
        log.info("Received void pre-auth request for {}", preauthId);
        paymentService.voidPreAuth(preauthId);
        return ResponseEntity.ok().build();
    }
}