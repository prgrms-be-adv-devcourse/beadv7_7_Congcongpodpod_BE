package kr.lastdish.core.order.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
    name = "orders",
    indexes = {
      @Index(
          name = "idx_orders_member_deleted_created_at",
          columnList = "member_id, is_deleted, created_at"),
      @Index(
          name = "idx_orders_store_deleted_created_at",
          columnList = "store_id, is_deleted, created_at")
    })
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
  private OrderRejectReason rejectReason;

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

  @Column(nullable = false)
  private LocalTime pickupEndAt;

  @Column(nullable = false)
  private LocalDateTime pickupDeadline;

  private LocalDateTime pickupResultAt;

  @Column(nullable = false)
  private BigDecimal totalPrice;

  @Column(nullable = false)
  private BigDecimal unitPrice;

  /**
   * 이 주문으로 아낀 총 금액입니다. (정가 - 판매가) × 수량으로 주문 시점에 확정한다.
   *
   * <p>등급·적립률 통계의 근거라 파생값이지만 여기서는 저장한다 — 나중에 Dish 가격이 바뀌어도 이미 끝난 주문의 적립 근거는 흔들리면 안 되기 때문이다.
   */
  @Column(nullable = false)
  private BigDecimal totalSavedAmount;

  @Column(nullable = false)
  private String dishName;

  @Column(nullable = false)
  private Long quantity;

  private String cancelReason;

  @Column(nullable = false)
  private Boolean isDeleted;

  @Column(nullable = false)
  private long eventVersion;

  // 주문 생성
  public static Order create(
      Long memberId,
      Long storeId,
      Long dishId,
      String memberName,
      String phone,
      String dishName,
      Long quantity,
      BigDecimal dishPrice,
      BigDecimal unitPrice,
      LocalTime pickupStartAt,
      LocalTime pickupEndAt,
      LocalDateTime pickupDeadline) {
    Order order = new Order();
    order.memberId = memberId;
    order.storeId = storeId;
    order.dishId = dishId;
    order.status = OrderStatus.RESERVED;
    order.paymentStatus = PaymentStatus.PENDING;
    order.memberName = memberName;
    order.phone = phone;
    order.dishName = dishName;
    order.quantity = quantity;
    order.unitPrice = unitPrice;
    order.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
    order.totalSavedAmount = dishPrice.subtract(unitPrice).multiply(BigDecimal.valueOf(quantity));
    order.pickupStartAt = pickupStartAt;
    order.pickupEndAt = pickupEndAt;
    order.pickupDeadline = pickupDeadline;
    order.isDeleted = false;
    order.eventVersion = 0L;
    return order;
  }

  public long nextEventVersion() {
    return ++eventVersion;
  }

  // 결제 완료
  public void paymentSuccess() {
    if (this.paymentStatus != PaymentStatus.PENDING) {
      throw new BusinessException(ErrorCode.INVALID_ORDER_PAYMENT_STATUS);
    }
    this.paymentStatus = PaymentStatus.COMPLETED;
  }

  // 픽업 코드 발급 - 픽업 대기 상태 변경
  public void issuePickupCode(String pickupCode) {
    if (this.paymentStatus != PaymentStatus.COMPLETED) {
      throw new BusinessException(CommonErrorCode.INVALID_STATE);
    }

    if (this.pickupCode != null) {
      throw new BusinessException(CommonErrorCode.INVALID_STATE);
    }

    transitionTo(OrderStatus.PICKUP_READY);
    this.pickupCode = pickupCode;
  }

  // 매장 주문 반려
  public void rejectOrder(OrderRejectReason reason) {
    transitionTo(OrderStatus.REJECTED);
    this.rejectReason = Objects.requireNonNull(reason);
  }

  public void delete() {
    this.isDeleted = true;
  }

  // 주문 취소
  public void cancel(Long memberId) {
    validateOwner(memberId);
    transitionTo(OrderStatus.CANCELLED);
  }

  public void validateOwner(Long memberId) {
    if (!Objects.equals(this.memberId, memberId)) {
      throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
    }
  }

  public void completePickup(LocalDateTime pickupResultAt) {
    if (pickupResultAt == null) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "픽업 완료 시각은 필수입니다.");
    }
    transitionTo(OrderStatus.PICKED_UP);
    this.pickupResultAt = pickupResultAt;
  }

  public void markNoShow(LocalDateTime pickupResultAt) {
    if (pickupResultAt == null) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "노쇼 처리 시각은 필수입니다.");
    }
    if (pickupResultAt.isBefore(this.pickupDeadline)) {
      throw new BusinessException(ErrorCode.ORDER_PICKUP_TIME_NOT_ENDED);
    }
    transitionTo(OrderStatus.NO_SHOW);
    this.pickupResultAt = pickupResultAt;
  }

  private void transitionTo(OrderStatus nextStatus) {
    if (!this.status.canTransitionTo(nextStatus)) {
      throw new BusinessException(CommonErrorCode.INVALID_STATE);
    }
    this.status = nextStatus;
  }
}
