package kr.lastdish.core.dish.application;

import java.util.Optional;
import kr.lastdish.core.dish.application.dto.DishSnapshot;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.domain.DishStatus;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.dish.presentation.dto.DishUpdateRequest;
import kr.lastdish.core.store.application.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DishFacade {

  private final DishRepository dishRepository;
  private final DishService dishService;
  private final StoreService storeService;

  @Transactional
  public DishResponse createDish(DishCreateRequest request) {
    storeService.validateDishPickupTime(
        request.storeId(), request.pickupStartTime(), request.pickupEndTime());
    return dishService.createDish(request);
  }

  @Transactional
  public DishResponse updateDish(Long dishId, DishUpdateRequest request) {
    Dish dish = dishRepository.findByIdAndIsDeletedFalse(dishId);
    storeService.validateDishPickupTime(
        dish.getStoreId(), request.pickupStartTime(), request.pickupEndTime());
    return dishService.updateDish(dishId, request);
  }

  /**
   * 조회 가능한(판매중) Dish의 스냅샷을 가져온다.
   *
   * <p>Optional을 반환하는 이유: 이 메서드는 "판매 가능한 Dish가 존재하는가"라는 사실만 확인한다. "없을 때 어떤 에러(404/409 등)로 처리할지"는 이
   * 데이터를 실제로 쓰는 호출자(예: CartService)가 자기 맥락에 맞게 결정할 문제이므로, 이 메서드에서 예외를 던지거나 null을 반환해 그 판단을 대신 내리지
   * 않는다.
   *
   * <p>호출자는 반드시 .orElseThrow(...) 등으로 "없음"의 의미를 직접 정해야 한다.
   */
  public Optional<DishSnapshot> findDishSnapshot(Long dishId) {
    return dishRepository
        .findAvailableById(dishId)
        .filter(dish -> dish.getDishStatus() == DishStatus.ON_SALE)
        .map(DishFacade::toSnapshot);
  }

  // 마감할인 서비스 특성상 스냅샷 단가는 discountPrice로 잡는다.
  private static DishSnapshot toSnapshot(Dish dish) {

    return new DishSnapshot(
        dish.getId(),
        dish.getStoreId(),
        dish.getDishName(),
        dish.getDiscountPrice(),
        dish.getTotalPrice(),
        dish.getSavedAmount(),
        dish.getStockQuantity(),
        dish.getPickupStartTime(),
        dish.getPickupEndTime(),
        dish.getAggregateVersion());
  }

  public void decreaseStock(Long dishId, Long quantity) {
    dishService.decreaseStock(dishId, quantity);
  }

  public void increaseStock(Long dishId, Long quantity) {
    dishService.increaseStock(dishId, quantity);
  }
}
