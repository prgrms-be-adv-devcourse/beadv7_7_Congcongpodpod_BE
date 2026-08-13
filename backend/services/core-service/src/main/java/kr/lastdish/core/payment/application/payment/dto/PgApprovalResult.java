package kr.lastdish.core.payment.application.payment.dto;

import java.math.BigDecimal;

public record PgApprovalResult(
    String pgTransactionId, // Toss의 paymentKey를 우리 도메인 용어로 담음
    boolean success,
    BigDecimal approvedAmount,
    String failureCode, // 실패 시 Toss 에러 코드 (성공 시 null)
    String failureMessage, // 실패 시 Toss 에러 메시지 (성공 시 null)
    String paymentMethod,
    String maskedCardNumber,
    String issuerCode) {
  public static PgApprovalResult success(
      String pgTransactionId,
      BigDecimal approvedAmount,
      String paymentMethod,
      String maskedCardNumber,
      String issuerCode) {
    return new PgApprovalResult(
        pgTransactionId,
        true,
        approvedAmount,
        null,
        null,
        paymentMethod,
        maskedCardNumber,
        issuerCode);
  }

  public static PgApprovalResult failure(
      String pgTransactionId, String failureCode, String failureMessage) {
    return new PgApprovalResult(
        pgTransactionId, false, null, failureCode, failureMessage, null, null, null);
  }
}
