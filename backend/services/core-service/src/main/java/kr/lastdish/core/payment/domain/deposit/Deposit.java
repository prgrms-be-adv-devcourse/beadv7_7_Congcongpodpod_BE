package kr.lastdish.core.payment.domain.deposit;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "deposits", uniqueConstraints = @UniqueConstraint(columnNames = "member_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deposit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "deposit_id")
  private Long id;

  @Column(name = "member_id", nullable = false, updatable = false)
  private Long memberId;

  @Column(name = "balance", precision = 19, scale = 4, nullable = false)
  private BigDecimal balance;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public Deposit(Long memberId, BigDecimal balance) {
    this.memberId = memberId;
    this.balance = balance;
    this.updatedAt = LocalDateTime.now(); // 생성될 때 현재 시간 자동 세팅
  }

  public static Deposit createDefault(Long memberId) {
    return new Deposit(memberId, BigDecimal.ZERO);
  }
}
