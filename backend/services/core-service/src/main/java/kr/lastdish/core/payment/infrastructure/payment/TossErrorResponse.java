package kr.lastdish.core.payment.infrastructure.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TossErrorResponse(String code, String message) {}
