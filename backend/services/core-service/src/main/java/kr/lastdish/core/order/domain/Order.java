package kr.lastdish.core.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @Column(nullable = false)
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
            String memberName,
            String phone,
            String dishName,
            Long quantity,
            BigDecimal unitPrice,
            BigDecimal totalPrice,
            LocalTime pickupStartAt,
            LocalTime pickupEndAt
    ) {
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
        order.totalPrice = totalPrice;
        order.pickupStartAt = pickupStartAt;
        order.pickupEndAt = pickupEndAt;
        order.isDeleted = false;
        return order;
    }

    // 결제 완료
    public void paymentSuccess() {
        this.paymentStatus = PaymentStatus.COMPLETED;
    }

    // 결제 실패
    public void paymentFailed() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    public void delete() {
        this.isDeleted = true;
    }

    // 주문 취소
    public void cancel(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
