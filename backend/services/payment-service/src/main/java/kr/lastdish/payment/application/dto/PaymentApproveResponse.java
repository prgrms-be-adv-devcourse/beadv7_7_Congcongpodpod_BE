package kr.lastdish.payment.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.payment.domain.Payment;

public record PaymentApproveResponse(
    Long paymentId,
    String merchantOrderId,
    BigDecimal amount,
    String approvedStatus,
    LocalDateTime approvedAt,
    String depositChargeMessage) {

  public static PaymentApproveResponse of(Payment payment, String depositChargeMessage) {
    return new PaymentApproveResponse(
        payment.getId(),
        payment.getMerchantOrderId(),
        payment.getAmount(),
        payment.getApprovedStatus().name(),
        payment.getApprovedAt(),
        depositChargeMessage);
  }
}
