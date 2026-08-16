package com.swiftlogistics.common.model;

public record OrderItem(
        String sku,
        String description,
        int quantity
) {
}
