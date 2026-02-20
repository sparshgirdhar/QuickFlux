package com.quickflux.orderservice.service;

public class OrderCreationFailedException extends RuntimeException {
    public OrderCreationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}