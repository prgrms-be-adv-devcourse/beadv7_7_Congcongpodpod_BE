package kr.lastdish.core.dish.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import kr.lastdish.core.common.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dishes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dish {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long storeId;

  @Column(nullable = false)
  private String dishName;

  @Column(nullable = false)
  private LocalDateTime registeredAt;

  @Column(nullable = false)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Category category;

  private String thumbnailUrl;

  @Column(nullable = false)
  private Long stockQuantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DishStatus dishStatus;

  @Column(nullable = false)
  private BigDecimal dishPrice;

  private BigDecimal discountPrice;

  private LocalTime pickupStartTime;

  private LocalTime pickupEndTime;

  @Column(nullable = false)
  private Boolean isDeleted;

  public static Dish create(
      Long storeId,
      String dishName,
      LocalDateTime registeredAt,
      String description,
      Category category,
      String thumbnailUrl,
      Long stockQuantity,
      BigDecimal dishPrice,
      BigDecimal discountPrice) {
    Dish dish = new Dish();
    dish.storeId = storeId;
    dish.dishName = dishName;
    dish.registeredAt = registeredAt;
    dish.description = description;
    dish.category = category;
    dish.thumbnailUrl = thumbnailUrl;
    dish.stockQuantity = stockQuantity;
    dish.dishStatus = DishStatus.ON_SALE;
    dish.dishPrice = dishPrice;
    dish.discountPrice = discountPrice;
    dish.isDeleted = false;
    return dish;
  }

  public void update(
      String dishName,
      LocalDateTime registeredAt,
      String description,
      Category category,
      String thumbnailUrl,
      Long stockQuantity,
      BigDecimal dishPrice,
      BigDecimal discountPrice) {
    this.dishName = dishName;
    this.registeredAt = registeredAt;
    this.description = description;
    this.category = category;
    this.thumbnailUrl = thumbnailUrl;
    this.stockQuantity = stockQuantity;
    this.dishPrice = dishPrice;
    this.discountPrice = discountPrice;

    if (stockQuantity == 0L) {
      this.dishStatus = DishStatus.SOLD_OUT;
    }
  }

  public void updateStatus(DishStatus dishStatus) {
    if (dishStatus == DishStatus.ON_SALE && this.stockQuantity <= 0) {
      throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
    }
    this.dishStatus = dishStatus;
  }

  public void decreaseStock(Long quantity) {
    validateOnSale();

    if (quantity == null || quantity <= 0) {
      throw new BusinessException(ErrorCode.INVALID_STOCK_QUANTITY);
    }

    if (this.stockQuantity < quantity) {
      throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
    }

    this.stockQuantity -= quantity;

    if (this.stockQuantity == 0L) {
      this.dishStatus = DishStatus.SOLD_OUT;
    }
  }

  private void validateOnSale() {
    if (this.dishStatus != DishStatus.ON_SALE) {
      throw new BusinessException(ErrorCode.DISH_NOT_ON_SALE);
    }
  }

  public void delete() {
    this.isDeleted = true;
  }

  /**
   * 현재 Dish가 사용자에게 판매 가능한 상태인지 판단합니다.
   *
   * <p>다음 조건을 모두 만족해야 판매 가능한 상품입니다.
   *
   * <ul>
   *   <li>Soft Delete되지 않음
   *   <li>판매 상태가 ON_SALE
   *   <li>재고 수량이 1개 이상
   * </ul>
   *
   * <p>판매 가능 여부 판단 규칙을 Dish 엔티티에 모아 Application 계층에서 동일한 규칙을 반복해서 구현하지 않게 합니다.
   */
  public boolean isAvailable() {
    return Boolean.FALSE.equals(isDeleted)
        && dishStatus == DishStatus.ON_SALE
        && stockQuantity != null
        && stockQuantity > 0;
  }
}
