package kr.lastdish.payment.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.payment.exception.ErrorCode;
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

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "approved_status", nullable = false)
  private ApprovedStatus approvedStatus;

  // PG사에서 결제 승인 후 가맹점에 발급하는 paymentKey
  @Column(name = "pg_transaction_id")
  private String pgTransactionId;

  @Column(name = "merchant_order_id", nullable = false, unique = true)
  private String merchantOrderId;

  @Column(name = "aggregate_version", nullable = false)
  private long aggregateVersion;

  private Payment(Long memberId, BigDecimal amount, PgProvider pgProvider, String merchantOrderId) {
    validatePositiveAmount(amount);
    this.memberId = memberId;
    this.amount = amount;
    this.pgProvider = pgProvider;
    this.merchantOrderId = merchantOrderId;
    this.approvedStatus = ApprovedStatus.READY;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
    this.aggregateVersion = 0L;
  }

  // 결제 준비
  public static Payment ready(
      Long memberId, BigDecimal amount, PgProvider pgProvider, String paymentRequestId) {
    return new Payment(memberId, amount, pgProvider, paymentRequestId);
  }

  // 결제 준비(READY) 상태를 확인하고 처리 중(CONFIRMING) 상태로 전환
  public ApprovalClaimResult claimApproval() {
    ApprovalClaimResult result =
        switch (this.approvedStatus) {
          case READY -> {
            this.approvedStatus = ApprovedStatus.PROCESSING;
            yield ApprovalClaimResult.STARTED;
          }
          case PROCESSING -> ApprovalClaimResult.ALREADY_PROCESSING;
          case APPROVED -> ApprovalClaimResult.ALREADY_APPROVED;
          case FAILED -> ApprovalClaimResult.ALREADY_FAILED;
          case EXPIRED ->
              throw new PaymentException(
                  ErrorCode.INVALID_PAYMENT_STATUS,
                  "만료된 결제입니다. 처음부터 다시 시도해주세요. paymentId=" + this.id);
        };
    if (result == ApprovalClaimResult.STARTED) {
      this.updatedAt = LocalDateTime.now();
    }
    return result;
  }

  // 결제 최종 승인 처리
  public void approve(String pgTransactionId) {
    if (this.approvedStatus != ApprovedStatus.PROCESSING) {
      throw new PaymentException(
          ErrorCode.INVALID_PAYMENT_STATUS, "승인 처리 중인 결제만 승인 완료할 수 있습니다. paymentId=" + this.id);
    }
    this.approvedStatus = ApprovedStatus.APPROVED;
    this.pgTransactionId = pgTransactionId;
    this.approvedAt = LocalDateTime.now();
    this.updatedAt = this.approvedAt;
  }

  // 결제 실패 처리
  public void fail() {
    if (this.approvedStatus != ApprovedStatus.PROCESSING) {
      throw new PaymentException(
          ErrorCode.INVALID_PAYMENT_STATUS, "승인 처리 중인 결제만 실패 처리할 수 있습니다. paymentId=" + this.id);
    }
    this.approvedStatus = ApprovedStatus.FAILED;
    this.updatedAt = LocalDateTime.now();
  }

  // Outbox 이벤트 발행 시 사용할 Aggregate 버전 증가
  public long nextAggregateVersion() {
    return ++this.aggregateVersion;
  }

  // 결제 금액 검증
  private void validatePositiveAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다. amount=" + amount);
    }
  }
}
