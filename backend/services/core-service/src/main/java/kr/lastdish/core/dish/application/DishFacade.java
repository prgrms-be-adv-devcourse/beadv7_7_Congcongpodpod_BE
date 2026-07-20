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

  // 장바구니 추가를 위해 Dish의 정보를 조회. 삭제됐거나 판매중이 아닌 Dish는 담을 수 없어야 하므로 걸러낸다.
  public Optional<DishSnapshot> findDishSnapshot(Long dishId) {
    return dishRepository
        .findById(dishId)
        .filter(dish -> !dish.getIsDeleted() && dish.getDishStatus() == DishStatus.ON_SALE)
        .map(DishFacade::toSnapshot);
  }

  // 마감할인 서비스 특성상 스냅샷 단가는 discountPrice로 잡는다.
  private static DishSnapshot toSnapshot(Dish dish) {
    return new DishSnapshot(
        dish.getId(), dish.getDishName(), dish.getDiscountPrice(), dish.getStockQuantity());
  }
}
