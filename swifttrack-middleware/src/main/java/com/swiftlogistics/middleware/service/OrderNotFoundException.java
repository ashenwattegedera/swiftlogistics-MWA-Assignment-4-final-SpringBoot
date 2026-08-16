package com.swiftlogistics.middleware.service;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("No order with id " + orderId);
    }
}
