package kr.lastdish.core.payment.domain.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    if (this.approvedStatus != ApprovedStatus.READY) {
      throw new IllegalStateException("결제 대기(READY) 상태에서만 승인할 수 있습니다.");
    }
    this.approvedStatus = ApprovedStatus.APPROVED;
    this.pgTransactionId = pgTransactionId;
    this.approvedAt = LocalDateTime.now();
  }

  // 결제 실패 처리
  public void fail() {
    if (this.approvedStatus != ApprovedStatus.READY) {
      throw new IllegalStateException("결제 대기(READY) 상태에서만 실패 처리할 수 있습니다.");
    }
    this.approvedStatus = ApprovedStatus.FAILED;
  }

  // 결제 금액 검증
  private void validatePositiveAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다. amount=" + amount);
    }
  }
}
