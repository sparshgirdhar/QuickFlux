package com.quickflux.paymentservice.service;

public class PaymentPreAuthFailedException extends RuntimeException {
    public PaymentPreAuthFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}