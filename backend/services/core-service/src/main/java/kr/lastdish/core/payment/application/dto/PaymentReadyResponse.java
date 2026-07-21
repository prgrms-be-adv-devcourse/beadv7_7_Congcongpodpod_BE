package kr.lastdish.core.payment.application.dto;

import java.math.BigDecimal;
import kr.lastdish.core.payment.domain.payment.Payment;

public record PaymentReadyResponse(
    Long paymentId,
    String merchantOrderId,
    BigDecimal amount,
    String approvedStatus,
    String tossClientKey) {
  public static PaymentReadyResponse of(Payment payment, String tossClientKey) {
    return new PaymentReadyResponse(
        payment.getId(),
        payment.getMerchantOrderId(),
        payment.getAmount(),
        payment.getApprovedStatus().name(),
        tossClientKey);
  }
}
