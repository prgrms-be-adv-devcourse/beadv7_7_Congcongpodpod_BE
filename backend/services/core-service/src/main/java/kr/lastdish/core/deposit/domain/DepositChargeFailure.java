package kr.lastdish.core.deposit.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deposit_charge_failures")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositChargeFailure {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "deposit_charge_failure_id")
  private Long id;

  @Column(name = "member_id", nullable = false, updatable = false)
  private Long memberId;

  @Column(name = "payment_id", nullable = false, updatable = false)
  private Long paymentId;

  @Column(name = "amount", precision = 19, scale = 2, nullable = false, updatable = false)
  private BigDecimal amount;

  @Column(name = "error_message", length = 1000)
  private String errorMessage;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private FailureStatus status;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  public enum FailureStatus {
    PENDING,
    RESOLVED,
    IGNORED
  }

  @Builder(access = AccessLevel.PRIVATE)
  private DepositChargeFailure(
      Long memberId, Long paymentId, BigDecimal amount, String errorMessage) {
    this.memberId = memberId;
    this.paymentId = paymentId;
    this.amount = amount;
    this.errorMessage = errorMessage;
    this.status = FailureStatus.PENDING;
    this.createdAt = LocalDateTime.now();
  }

  public static DepositChargeFailure record(
      Long memberId, Long paymentId, BigDecimal amount, String errorMessage) {
    return DepositChargeFailure.builder()
        .memberId(memberId)
        .paymentId(paymentId)
        .amount(amount)
        .errorMessage(errorMessage)
        .build();
  }

  // 운영자 레벨에서 수동 크레딧으로 재처리 완료했을 때 호출
  public void resolve() {
    this.status = FailureStatus.RESOLVED;
    this.resolvedAt = LocalDateTime.now();
  }

  // 결제 취소/환불되어 예치금 처리 불필요하다고 판단될 때 호출
  public void ignore() {
    this.status = FailureStatus.IGNORED;
    this.resolvedAt = LocalDateTime.now();
  }
}
