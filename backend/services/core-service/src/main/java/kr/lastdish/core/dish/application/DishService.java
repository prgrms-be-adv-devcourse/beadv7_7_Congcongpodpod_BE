package kr.lastdish.core.dish.application;

import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.presentation.dto.DishStatusRequest;
import kr.lastdish.core.dish.presentation.dto.DishUpdateRequest;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class DishService {
  private final DishRepository dishRepository;

  @Transactional
  public DishResponse createDish(DishCreateRequest request) {

    // 할인율 검증
    validateDiscountRate(request.dishPrice(), request.discountPrice());

    Dish dish =
        Dish.create(
            request.storeId(),
            request.dishName(),
            request.registeredAt(),
            request.description(),
            request.category(),
            request.thumbnailUrl(),
            request.stockQuantity(),
            request.dishPrice(),
            request.discountPrice());

    Dish savedDish = dishRepository.save(dish);
    return DishResponse.from(savedDish);
  }

  @Transactional
  public DishResponse updateDish(Long dishId, DishUpdateRequest request) {
    // 할인율 검증
    validateDiscountRate(request.dishPrice(), request.discountPrice());

    Dish dish = getDish(dishId);
    dish.update(
        request.dishName(),
        request.registeredAt(),
        request.description(),
        request.category(),
        request.thumbnailUrl(),
        request.stockQuantity(),
        request.dishPrice(),
        request.discountPrice());
    return DishResponse.from(dish);
  }

  @Transactional
  public DishResponse updateDishStatus(Long dishId, DishStatusRequest request) {
    Dish dish = getDish(dishId);
    dish.updateStatus(request.dishStatus());
    return DishResponse.from(dish);
  }

  private void validateDiscountRate(BigDecimal dishPrice, BigDecimal discountPrice) {
    BigDecimal discountRate = dishPrice
            .subtract(discountPrice)
            .divide(dishPrice, 4, RoundingMode.HALF_UP);

    if (discountRate.compareTo(BigDecimal.valueOf(0.3)) < 0) {
      throw new IllegalArgumentException("할인율은 30% 이상이어야 합니다.");
    }
  }

  @Transactional
  public void deleteDish(Long dishId) {
    Dish dish = getDish(dishId);
    dish.delete();
  }

  public DishResponse getEachDish(Long dishId) {
    Dish dish = getDish(dishId);
    return DishResponse.from(dish);
  }

  private Dish getDish(Long dishId) {
    return dishRepository.findByIdAndIsDeletedFalse(dishId);
  }
}
