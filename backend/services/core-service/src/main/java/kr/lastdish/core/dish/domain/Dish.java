package kr.lastdish.core.dish.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @Column(nullable = false)
    private Category category;

    private String thumbnailUrl;

    @Column(nullable = false)
    private Long stockQuantity;

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
            BigDecimal discountPrice
    ) {
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
}
