package kr.lastdish.core.cart.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(columnNames = "cart_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long cartId;

  @Column(nullable = false)
  private Long dishId;

  @Column(nullable = false)
  private String dishName;

  @Column(nullable = false)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private Long quantity;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CartItemStatus status;

  private CartItem(Long cartId, Long dishId, String dishName, BigDecimal unitPrice, Long quantity) {
    this.cartId = cartId;
    this.dishId = dishId;
    this.dishName = dishName;
    this.unitPrice = unitPrice;
    this.quantity = quantity;

    // 초기값이 AVAILABLE인 이유는 Cart에 추가할 때 DishFacade를 통해 Dish 존재 여부와 재고를 확인하는걸로 확인했습니다.
    this.status = CartItemStatus.AVAILABLE;

    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static CartItem create(
      Long cartId, Long dishId, String dishName, BigDecimal unitPrice, Long quantity) {
    return new CartItem(cartId, dishId, dishName, unitPrice, quantity);
  }

  public void replace(Long dishId, String dishName, BigDecimal unitPrice, Long quantity) {
    this.dishId = dishId;
    this.dishName = dishName;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
    this.updatedAt = LocalDateTime.now();
  }

  public void changeQuantity(Long quantity) {
    if (quantity == null || quantity < 1) {
      throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
    }
    this.quantity = quantity;
    this.updatedAt = LocalDateTime.now();
  }

  public BigDecimal getSubtotalPrice() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
  }


  /**
   * 최신 Dish 상태와 재고를 기준으로 장바구니 상품 상태를 갱신합니다.
   *
   * @param dishAvailable Dish 자체의 판매 가능 여부
   * @param stockQuantity 현재 Dish 재고
   */
  public void synchronizeDishState(boolean dishAvailable, Long stockQuantity) {
    if (!dishAvailable) {
      this.status = CartItemStatus.DISH_UNAVAILABLE;
    } else if (stockQuantity == null || stockQuantity <= 0) {
      this.status = CartItemStatus.OUT_OF_STOCK;
    } else if (this.quantity > stockQuantity) {
      this.status = CartItemStatus.INSUFFICIENT_STOCK;
    } else {
      this.status = CartItemStatus.AVAILABLE;
    }

    this.updatedAt = LocalDateTime.now();
  }

  public boolean isOrderable() {
    return this.status == CartItemStatus.AVAILABLE;
  }
}
