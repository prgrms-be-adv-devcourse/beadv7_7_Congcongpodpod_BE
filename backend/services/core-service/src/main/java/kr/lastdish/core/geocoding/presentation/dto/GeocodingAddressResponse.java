package kr.lastdish.core.geocoding.presentation.dto;

import java.math.BigDecimal;

public record GeocodingAddressResponse(
    String roadAddress,
    String jibunAddress,
    String englishAddress,
    BigDecimal latitude,
    BigDecimal longitude) {}
