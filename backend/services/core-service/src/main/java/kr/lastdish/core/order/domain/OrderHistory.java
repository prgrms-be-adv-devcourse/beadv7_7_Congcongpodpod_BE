package kr.lastdish.core.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "order_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class OrderHistory {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long orderId;

  @Column(nullable = false)
  private Long memberId;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private OrderStatus status;

  @Column(nullable = false)
  private LocalDateTime orderUpdatedAt;

  @Column(nullable = false)
  @CreatedDate
  private LocalDateTime createdAt;

  public static OrderHistory create(
      Long orderId, Long memberId, OrderStatus status, LocalDateTime orderUpdatedAt) {
    OrderHistory history = new OrderHistory();
    history.orderId = orderId;
    history.memberId = memberId;
    history.status = status;
    history.orderUpdatedAt = orderUpdatedAt;
    return history;
  }
}
