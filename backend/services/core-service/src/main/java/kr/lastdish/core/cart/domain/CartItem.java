package kr.lastdish.core.cart.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

  private CartItem(Long cartId, Long dishId, String dishName, BigDecimal unitPrice, Long quantity) {
    this.cartId = cartId;
    this.dishId = dishId;
    this.dishName = dishName;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
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
}
