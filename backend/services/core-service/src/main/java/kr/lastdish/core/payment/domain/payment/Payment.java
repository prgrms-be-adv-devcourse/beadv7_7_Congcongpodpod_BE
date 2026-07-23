package kr.lastdish.core.payment.domain.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "payment_id")
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(nullable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "pg_provider", nullable = false)
  private PgProvider pgProvider;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "approved_status", nullable = false)
  private ApprovedStatus approvedStatus;

  @Column(name = "pg_transaction_id")
  private String pgTransactionId;

  @Column(name = "merchant_order_id", nullable = false, unique = true)
  private String merchantOrderId;

  private Payment(Long memberId, BigDecimal amount, PgProvider pgProvider, String merchantOrderId) {
    validatePositiveAmount(amount);
    this.memberId = memberId;
    this.amount = amount;
    this.pgProvider = pgProvider;
    this.merchantOrderId = merchantOrderId;
    this.approvedStatus = ApprovedStatus.READY;
    this.createdAt = LocalDateTime.now();
  }

  // 결제 대기
  public static Payment ready(
      Long memberId, BigDecimal amount, PgProvider pgProvider, String paymentRequestId) {
    return new Payment(memberId, amount, pgProvider, paymentRequestId);
  }

  // 결제 최종 승인 처리
  public void approve(String pgTransactionId) {
    validateReadyStatus();
    this.approvedStatus = ApprovedStatus.APPROVED;
    this.pgTransactionId = pgTransactionId;
    this.approvedAt = LocalDateTime.now();
  }

  // 결제 실패 처리
  public void fail() {
    validateReadyStatus();
    this.approvedStatus = ApprovedStatus.FAILED;
  }

  // 결제 금액 검증
  private void validatePositiveAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다. amount=" + amount);
    }
  }

  // 결제 준비(READY) 상태인지 검증
  private void validateReadyStatus() {
    if (this.approvedStatus != ApprovedStatus.READY) {
      throw new PaymentException(ErrorCode.INVALID_PAYMENT_STATUS, buildStatusMessage());
    }
  }

  private String buildStatusMessage() {
    String detail =
        switch (this.approvedStatus) {
          case APPROVED -> "이미 승인된 결제입니다.";
          case FAILED -> "실패한 결제입니다. 결제를 다시 시도해주세요.";
          default -> "처리할 수 없는 결제 상태입니다.";
        };
    return detail + " paymentId=" + this.id;
  }
}
