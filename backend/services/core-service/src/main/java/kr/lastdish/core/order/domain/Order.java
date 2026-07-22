package kr.lastdish.core.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import kr.lastdish.core.common.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long memberId;

  @Column(nullable = false)
  private Long storeId;

  @Column(nullable = false)
  private Long dishId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus paymentStatus;

  @Column(nullable = false)
  @CreatedDate
  private LocalDateTime createdAt;

  @Column(nullable = false)
  @LastModifiedDate
  private LocalDateTime updatedAt;

  private String memberName;

  @Column(nullable = false)
  private String phone;

  private String pickupCode;

  private LocalTime pickupStartAt;

  private LocalTime pickupEndAt;

  @Column(nullable = false)
  private BigDecimal totalPrice;

  @Column(nullable = false)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private String dishName;

  @Column(nullable = false)
  private Long quantity;

  private String cancelReason;

  @Column(nullable = false)
  private Boolean isDeleted;

  // 주문 생성
  public static Order create(
      Long memberId,
      Long storeId,
      Long dishId,
      // String memberName,
      String phone,
      String dishName,
      Long quantity,
      BigDecimal unitPrice,
      LocalTime pickupStartAt,
      LocalTime pickupEndAt) {
    Order order = new Order();
    order.memberId = memberId;
    order.storeId = storeId;
    order.dishId = dishId;
    order.status = OrderStatus.RESERVED;
    order.paymentStatus = PaymentStatus.PENDING;
    // order.memberName = memberName;
    order.phone = phone;
    order.dishName = dishName;
    order.quantity = quantity;
    order.unitPrice = unitPrice;
    order.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    order.pickupStartAt = pickupStartAt;
    order.pickupEndAt = pickupEndAt;
    order.isDeleted = false;
    return order;
  }

  // 결제 완료
  public void paymentSuccess() {
    this.paymentStatus = PaymentStatus.COMPLETED;
  }

  public void delete() {
    this.isDeleted = true;
  }

  // 주문 취소
  public void cancel(Long memberId, String cancelReason) {
    validateOwner(memberId);
    validateCancelable();

    this.cancelReason = cancelReason;
    this.status = OrderStatus.CANCELLED;
  }

  private void validateCancelable() {
    if (this.status != OrderStatus.RESERVED) {
      throw new BusinessException(ErrorCode.INVALID_STATE);
    }
  }

  private void validateOwner(Long memberId) {
    if (!Objects.equals(this.memberId, memberId)) {
      throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
    }
  }
}
