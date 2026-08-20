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

  @Column(nullable = false)
  private Long storeId;

  @Column(nullable = false)
  private String dishName;

  /** 할인 전 정가입니다. 주문 시 절약 금액(정가 - 판매가)을 계산하는 데 쓰입니다. */
  @Column(nullable = false)
  private BigDecimal dishPrice;

  /** 실제로 결제하는 판매가(마감 할인가)입니다. */
  @Column(nullable = false)
  private BigDecimal unitPrice;

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
      BigDecimal dishPrice,
      BigDecimal unitPrice,
      Long quantity,
      LocalTime pickupStartAt,
      LocalTime pickupEndAt,
      long dishVersion) {
    this.cartId = cartId;
    this.dishId = dishId;
    this.storeId = storeId;
    this.dishName = dishName;
    this.dishPrice = dishPrice;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
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
      BigDecimal dishPrice,
      BigDecimal unitPrice,
      Long quantity,
      LocalTime pickupStartAt,
      LocalTime pickupEndAt,
      long dishVersion) {
    validateQuantity(quantity);
    validatePrices(dishPrice, unitPrice);

    return new CartItem(
        cartId,
        dishId,
        storeId,
        dishName,
        dishPrice,
        unitPrice,
        quantity,
        pickupStartAt,
        pickupEndAt,
        dishVersion);
  }

  public void replace(
      Long dishId,
      Long storeId,
      String dishName,
      BigDecimal dishPrice,
      BigDecimal unitPrice,
      Long quantity,
      LocalTime pickupStartAt,
      LocalTime pickupEndAt,
      long dishVersion) {
    validateQuantity(quantity);
    validatePrices(dishPrice, unitPrice);

    this.dishId = dishId;
    this.storeId = storeId;
    this.dishName = dishName;
    this.dishPrice = dishPrice;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
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

  /** 주문 시 절약 금액을 정가 - 판매가로 계산하므로, 정가가 판매가보다 낮으면 음수 절약 금액이 만들어진다. */
  private static void validatePrices(BigDecimal dishPrice, BigDecimal unitPrice) {
    if (unitPrice == null || unitPrice.signum() < 0) {
      throw new IllegalArgumentException("Dish 판매 가격은 0 이상이어야 합니다.");
    }

    if (dishPrice == null || dishPrice.compareTo(unitPrice) < 0) {
      throw new IllegalArgumentException("Dish 정가는 판매 가격 이상이어야 합니다.");
    }
  }

  public BigDecimal getSubtotalPrice() {
    return unitPrice.multiply(BigDecimal.valueOf(quantity));
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
   * 최신 Dish 가격 이벤트를 CartItem의 정가와 단가에 반영합니다.
   *
   * <p>정가도 함께 갱신하는 이유: 주문 시 절약 금액을 정가 - 판매가로 계산하므로, 정가가 낡으면 적립 통계가 조용히 틀어진다.
   *
   * @param dishPrice Dish의 현재 정가
   * @param unitPrice Dish의 현재 판매 가격
   * @param aggregateVersion Dish 가격 변경 이벤트 순서
   */
  public void synchronizeDishPrice(
      BigDecimal dishPrice, BigDecimal unitPrice, long aggregateVersion) {

    if (aggregateVersion <= this.lastAppliedDishPriceVersion) {
      return;
    }

    validatePrices(dishPrice, unitPrice);

    this.dishPrice = dishPrice;
    this.unitPrice = unitPrice;
    this.lastAppliedDishPriceVersion = aggregateVersion;
    this.updatedAt = LocalDateTime.now();
  }

  public boolean isOrderable() {
    return this.status == CartItemStatus.AVAILABLE;
  }
}
