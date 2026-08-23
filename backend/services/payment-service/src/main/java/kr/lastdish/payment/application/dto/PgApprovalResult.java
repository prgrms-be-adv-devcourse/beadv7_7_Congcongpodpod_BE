package kr.lastdish.payment.application.dto;

import java.math.BigDecimal;

public record PgApprovalResult(
    String pgTransactionId, // Toss의 paymentKey를 우리 도메인 용어로 담음
    Status status,
    BigDecimal approvedAmount,
    String failureCode, // 실패 시 Toss 에러 코드 (성공 시 null)
    String failureMessage, // 실패 시 Toss 에러 메시지 (성공 시 null)
    String paymentMethod,
    String maskedCardNumber,
    String issuerCode) {
  public enum Status {
    SUCCESS,
    FAILURE,
    UNKNOWN // Toss 응답도, 재조회 응답도 받지 못해 결과를 확정할 수 없는 상태
  }
  public static PgApprovalResult success(
          String pgTransactionId,
          BigDecimal approvedAmount,
          String paymentMethod,
          String maskedCardNumber,
          String issuerCode) {
    return new PgApprovalResult(
            pgTransactionId,
            Status.SUCCESS,
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
        pgTransactionId, Status.FAILURE, null, failureCode, failureMessage, null, null, null);
  }

  public static PgApprovalResult unknown(String pgTransactionId, String reason) {
    return new PgApprovalResult(
            pgTransactionId, Status.UNKNOWN, null, "UNKNOWN", reason, null, null, null);
  }
}
