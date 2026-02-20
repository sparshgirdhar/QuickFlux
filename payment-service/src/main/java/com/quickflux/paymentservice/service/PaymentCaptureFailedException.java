package com.quickflux.paymentservice.service;

public class PaymentCaptureFailedException extends RuntimeException {
    public PaymentCaptureFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
