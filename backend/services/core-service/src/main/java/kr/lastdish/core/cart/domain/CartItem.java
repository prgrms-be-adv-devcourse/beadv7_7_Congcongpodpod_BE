package kr.lastdish.core.cart.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

  @Column private Long storeId;

  @Column(nullable = false)
  private String dishName;

  @Column(nullable = false)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private BigDecimal unitSavedAmount;

  @Column(nullable = false)
  private BigDecimal totalSavedAmount;

  @Column(nullable = false)
  private Long quantity;

  @Column(nullable = false)
  private LocalTime pickupStartAt;

  @Column(nullable = false)
  private LocalTime pickupEndAt;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CartItemStatus status;

  @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
  private long lastAppliedDishVersion;

  @Column(nullable = false, columnDefinition = "BIGINT DEFAULT 0")
  private long lastAppliedDishPriceVersion;

  private CartItem(
      Long cartId,
      Long dishId,
      Long storeId,
      String dishName,
      BigDecimal unitPrice,
      BigDecimal unitSavedAmount,
      BigDecimal totalSavedAmount,
      Long quantity,
      LocalTime pickupStartAt,
      LocalTime pickupEndAt,
      long dishVersion) {
    this.cartId = cartId;
    this.dishId = dishId;
    this.storeId = storeId;
    this.dishName = dishName;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
    this.unitSavedAmount = unitSavedAmount;
    this.totalSavedAmount = totalSavedAmount;
    this.pickupStartAt = pickupStartAt;
    this.pickupEndAt = pickupEndAt;
    this.lastAppliedDishVersion = dishVersion;
    this.lastAppliedDishPriceVersion = dishVersion;

    // 초기값이 AVAILABLE인 이유는 Cart에 추가할 때 DishFacade를 통해 Dish 존재 여부와 재고를 확인하는걸로 확인했습니다.
    this.status = CartItemStatus.AVAILABLE;

    LocalDateTime now = LocalDateTime.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  public static CartItem create(
      Long cartId,
      Long dishId,
      Long storeId,
      String dishName,
      BigDecimal unitPrice,
      BigDecimal unitSavedAmount,
      Long quantity,
      LocalTime pickupStartAt,
      LocalTime pickupEndAt,
      long dishVersion) {
    return new CartItem(
        cartId,
        dishId,
        storeId,
        dishName,
        unitPrice,
        unitSavedAmount,
        unitSavedAmount.multiply(BigDecimal.valueOf(quantity)),
        quantity,
        pickupStartAt,
        pickupEndAt,
        dishVersion);
  }

  /**
   * @deprecated 총 가격은 단가와 수량으로 계산됩니다. {@code totalPrice}를 받지 않는 팩터리를 사용하세요.
   */
  @Deprecated(forRemoval = true)
  public static CartItem create(
      Long cartId,
      Long dishId,
      Long storeId,
      String dishName,
      BigDecimal unitPrice,
      BigDecimal totalPrice,
      BigDecimal unitSavedAmount,
      Long quantity,
      LocalTime pickupStartAt,
      LocalTime pickupEndAt,
      long dishVersion) {
    return create(
        cartId,
        dishId,
        storeId,
        dishName,
        unitPrice,
        unitSavedAmount,
        quantity,
        pickupStartAt,
        pickupEndAt,
        dishVersion);
  }

  public void replace(
      Long dishId,
      Long storeId,
      String dishName,
      BigDecimal unitPrice,
      BigDecimal unitSavedAmount,
      Long quantity,
      LocalTime pickupStartAt,
      LocalTime pickupEndAt,
      long dishVersion) {
    this.dishId = dishId;
    this.storeId = storeId;
    this.dishName = dishName;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
    this.unitSavedAmount = unitSavedAmount;
    this.totalSavedAmount = unitSavedAmount.multiply(BigDecimal.valueOf(quantity));
    this.pickupStartAt = pickupStartAt;
    this.pickupEndAt = pickupEndAt;
    this.lastAppliedDishVersion = dishVersion;
    this.lastAppliedDishPriceVersion = dishVersion;

    /*
     * CartService에서 교체할 Dish의 판매 여부와 재고를 검증한 뒤 호출하므로
     * 이전 Dish에서 파생된 주문 불가 상태를 유지하지 않습니다.
     */
    this.status = CartItemStatus.AVAILABLE;
    this.updatedAt = LocalDateTime.now();
  }

  public void changeQuantity(Long quantity) {
    changeQuantity(quantity, this.lastAppliedDishVersion);
  }

  public void changeQuantity(Long quantity, long dishVersion) {
    validateQuantity(quantity);

    this.quantity = quantity;
    this.totalSavedAmount = this.unitSavedAmount.multiply(BigDecimal.valueOf(quantity));
    this.lastAppliedDishVersion = dishVersion;

    /*
     * CartService에서 변경할 수량이 현재 Dish 재고 이내인지 검증한 뒤 호출하므로
     * 이전 수량에서 계산된 재고 부족 상태를 초기화합니다.
     */
    this.status = CartItemStatus.AVAILABLE;
    this.updatedAt = LocalDateTime.now();
  }

  private static void validateQuantity(Long quantity) {
    if (quantity == null || quantity < 1) {
      throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");
    }
  }

  public BigDecimal getTotalPrice() {
    return unitPrice;
  }

  public BigDecimal getSubtotalPrice() {
    return getTotalPrice();
  }

  /**
   * 최신 Dish 상태와 재고를 기준으로 장바구니 상품 상태를 갱신합니다.
   *
   * @param dishAvailable Dish 자체의 판매 가능 여부
   * @param stockQuantity 현재 Dish 재고
   * @param aggregateVersion Dish 상태 변경 순서
   */
  public void synchronizeDishState(
      boolean dishAvailable, Long stockQuantity, long aggregateVersion) {
    if (aggregateVersion <= this.lastAppliedDishVersion) {
      return;
    }

    if (!dishAvailable) {
      this.status = CartItemStatus.DISH_UNAVAILABLE;
    } else if (stockQuantity == null || stockQuantity <= 0) {
      this.status = CartItemStatus.OUT_OF_STOCK;
    } else if (this.quantity > stockQuantity) {
      this.status = CartItemStatus.INSUFFICIENT_STOCK;
    } else {
      this.status = CartItemStatus.AVAILABLE;
    }

    this.lastAppliedDishVersion = aggregateVersion;
    this.updatedAt = LocalDateTime.now();
  }

  /**
   * 최신 Dish 가격 이벤트를 CartItem 단가에 반영합니다.
   *
   * @param unitPrice Dish의 현재 판매 가격
   * @param aggregateVersion Dish 가격 변경 이벤트 순서
   */
  public void synchronizeDishPrice(BigDecimal unitPrice, long aggregateVersion) {

    if (aggregateVersion <= this.lastAppliedDishPriceVersion) {
      return;
    }

    if (unitPrice == null || unitPrice.signum() < 0) {
      throw new IllegalArgumentException("Dish 판매 가격은 0 이상이어야 합니다.");
    }

    this.unitPrice = unitPrice;
    this.lastAppliedDishPriceVersion = aggregateVersion;
    this.updatedAt = LocalDateTime.now();
  }

  public boolean isOrderable() {
    return this.status == CartItemStatus.AVAILABLE;
  }
}
