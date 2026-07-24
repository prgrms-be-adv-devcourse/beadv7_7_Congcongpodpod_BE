package kr.lastdish.core.payment.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.payment.domain.payment.Payment;

public record PaymentApproveResponse(
    Long paymentId,
    String merchantOrderId,
    BigDecimal amount,
    String approvedStatus,
    LocalDateTime approvedAt,
    BigDecimal depositBalance) {
  public static PaymentApproveResponse of(Payment payment, BigDecimal depositBalance) {
    return new PaymentApproveResponse(
        payment.getId(),
        payment.getMerchantOrderId(),
        payment.getAmount(),
        payment.getApprovedStatus().name(),
        payment.getApprovedAt(),
        depositBalance);
  }
}
