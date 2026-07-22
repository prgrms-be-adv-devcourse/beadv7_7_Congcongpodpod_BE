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

  public void increaseStock(Long quantity) {
    if (quantity == null || quantity <= 0) {
      throw new BusinessException(ErrorCode.INVALID_STOCK_QUANTITY);
    }

    this.stockQuantity += quantity;

    if (this.stockQuantity > 0 && this.dishStatus == DishStatus.SOLD_OUT) {
      this.dishStatus = DishStatus.ON_SALE;
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
}
