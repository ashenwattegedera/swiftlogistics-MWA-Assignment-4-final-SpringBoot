package com.swiftlogistics.common.model;

public record Address(
        String street,
        String city,
        String postalCode,
        Double latitude,
        Double longitude
) {
}
