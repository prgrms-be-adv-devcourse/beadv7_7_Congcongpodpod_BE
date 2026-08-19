package kr.lastdish.core.point.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "point_history_id")
  private Long id;

  @Column(name = "member_id", nullable = false, updatable = false)
  private Long memberId;

  @Column(name = "order_id")
  private Long orderId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private PointType type;

  @Column(name = "amount", precision = 19, scale = 4, nullable = false)
  private BigDecimal amount;

  // 아직 소멸되지 않고 남은 금액
  @Column(name = "remaining_amount", precision = 19, scale = 4)
  private BigDecimal remainingAmount;

  // 적립일 + 3개월 (소멸 날짜)
  @Column(name = "expires_at")
  private LocalDateTime expiresAt;

  @Column(name = "balance_after", precision = 19, scale = 4, nullable = false)
  private BigDecimal balanceAfter;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Builder(access = AccessLevel.PRIVATE)
  private PointHistory(
      Long memberId,
      Long orderId,
      PointType type,
      BigDecimal amount,
      BigDecimal remainingAmount,
      LocalDateTime expiresAt,
      BigDecimal balanceAfter) {
    this.memberId = memberId;
    this.orderId = orderId;
    this.type = type;
    this.amount = amount;
    this.remainingAmount = remainingAmount;
    this.expiresAt = expiresAt;
    this.balanceAfter = balanceAfter;
    this.createdAt = LocalDateTime.now();
  }

  public static PointHistory recordEarn(
      Long memberId, Long orderId, BigDecimal amount, BigDecimal balanceAfter) {
    validatePositiveAmount(amount);
    return PointHistory.builder()
        .memberId(memberId)
        .orderId(orderId)
        .type(PointType.EARN)
        .amount(amount)
        .remainingAmount(amount)
        .expiresAt(LocalDateTime.now().plusMonths(3).with(LocalTime.MAX))
        .balanceAfter(balanceAfter)
        .build();
  }

  public static PointHistory recordUse(
      Long memberId, Long orderId, BigDecimal amount, BigDecimal balanceAfter) {
    validatePositiveAmount(amount);
    return PointHistory.builder()
        .memberId(memberId)
        .orderId(orderId)
        .type(PointType.USE)
        .amount(amount)
        .balanceAfter(balanceAfter)
        .build();
  }

  // 해당 EARN 건에서 amount만큼 소진
  public void consume(BigDecimal amount) {
    validatePositiveAmount(amount);
    if (amount.compareTo(this.remainingAmount) > 0) {
      throw new IllegalStateException(
          "적립 건의 잔여 포인트보다 많은 금액을 소진할 수 없습니다. remaining="
              + this.remainingAmount
              + ", amount="
              + amount);
    }
    this.remainingAmount = this.remainingAmount.subtract(amount);
  }

  private static void validatePositiveAmount(BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(
          CommonErrorCode.INVALID_INPUT, "금액은 0보다 커야 합니다. amount=" + amount);
    }
  }
}
