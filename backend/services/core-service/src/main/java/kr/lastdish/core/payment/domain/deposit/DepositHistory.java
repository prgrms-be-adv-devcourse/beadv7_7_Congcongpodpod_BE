package kr.lastdish.core.payment.domain.deposit;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deposit_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "deposit_history_id")
  private Long id;

  @Column(name = "member_id", nullable = false)
  private Long memberId;

  @Column(name = "order_id")
  private Long orderId;

  @Column(name = "payment_id")
  private Long paymentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private DepositType type;

  @Column(name = "amount", precision = 19, scale = 4, nullable = false)
  private BigDecimal amount;

  @Column(name = "balance_after", precision = 19, scale = 4, nullable = false)
  private BigDecimal balanceAfter;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public enum DepositType {
    CHARGE,
    USE,
    REFUND
  }

  @Builder(access = AccessLevel.PRIVATE)
  private DepositHistory(
      Long memberId,
      Long orderId,
      Long paymentId,
      DepositType type,
      BigDecimal amount,
      BigDecimal balanceAfter) {
    this.memberId = memberId;
    this.orderId = orderId;
    this.paymentId = paymentId;
    this.type = type;
    this.amount = amount;
    this.balanceAfter = balanceAfter;
    this.createdAt = LocalDateTime.now();
  }

  public static DepositHistory recordCharge(
      Long memberId, Long paymentId, BigDecimal amount, BigDecimal balanceAfter) {
    return DepositHistory.builder()
        .memberId(memberId)
        .paymentId(paymentId)
        .type(DepositType.CHARGE)
        .amount(amount)
        .balanceAfter(balanceAfter)
        .build();
  }

  public static DepositHistory recordUsageOrRefund(
      Long memberId, Long orderId, DepositType type, BigDecimal amount, BigDecimal balanceAfter) {
    return DepositHistory.builder()
        .memberId(memberId)
        .orderId(orderId)
        .type(type)
        .amount(amount)
        .balanceAfter(balanceAfter)
        .build();
  }
}
