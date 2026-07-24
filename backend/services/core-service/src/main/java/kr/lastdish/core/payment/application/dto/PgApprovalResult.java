package kr.lastdish.core.payment.application.dto;

import java.math.BigDecimal;

public record PgApprovalResult(
    String pgTransactionId, // Toss의 paymentKey를 우리 도메인 용어로 담음
    boolean success,
    BigDecimal approvedAmount,
    String failureCode, // 실패 시 Toss 에러 코드 (성공 시 null)
    String failureMessage, // 실패 시 Toss 에러 메시지 (성공 시 null)
    String rawResponse) {
  public static PgApprovalResult success(
      String pgTransactionId, BigDecimal approvedAmount, String rawResponse) {
    return new PgApprovalResult(pgTransactionId, true, approvedAmount, null, null, rawResponse);
  }

  public static PgApprovalResult failure(
      String failureCode, String failureMessage, String rawResponse) {
    return new PgApprovalResult(null, false, null, failureCode, failureMessage, rawResponse);
  }
}
