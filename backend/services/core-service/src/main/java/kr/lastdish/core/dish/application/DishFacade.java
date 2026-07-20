package kr.lastdish.core.dish.application;

import java.util.Optional;
import kr.lastdish.core.dish.application.dto.DishSnapshot;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.domain.DishStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DishFacade {

  private final DishRepository dishRepository;

  public Optional<DishSnapshot> findDishSnapshot(Long dishId) {
    Dish dish;
    try {
      dish = dishRepository.findByIdAndIsDeletedFalse(dishId);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }

    // 여기부터는 dish가 존재함이 보장되므로, 판매중이 아니면 그냥 없음으로 취급한다.
    if (dish.getDishStatus() != DishStatus.ON_SALE) {
      return Optional.empty();
    }
    return Optional.of(toSnapshot(dish));
  }

  // 마감할인 서비스 특성상 스냅샷 단가는 discountPrice로 잡는다.
  private static DishSnapshot toSnapshot(Dish dish) {
    return new DishSnapshot(
        dish.getId(), dish.getDishName(), dish.getDiscountPrice(), dish.getStockQuantity());
  }
}
