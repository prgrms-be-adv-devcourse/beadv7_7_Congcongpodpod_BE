package kr.lastdish.core.dish.application;

import kr.lastdish.core.common.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.dish.presentation.dto.DishStatusRequest;
import kr.lastdish.core.dish.presentation.dto.DishUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DishService {
  private final DishRepository dishRepository;
  private final OutboxEventWriter outboxEventWriter;

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

    // 동시에 같은 Dish가 변경되면 동일한 event Version이 생성되지 않도록 이벤트가 발생하는 변경 메서드는 잠금 조회를 사용합니다.
    Dish dish = dishRepository.findWithLockByIdAndIsDeletedFalse(dishId);

    boolean availableBefore = dish.isAvailable();
    Long stockQuantityBefore = dish.getStockQuantity();

    dish.update(
        request.dishName(),
        request.registeredAt(),
        request.description(),
        request.category(),
        request.thumbnailUrl(),
        request.stockQuantity(),
        request.dishPrice(),
        request.discountPrice());

    appendStateEventIfChanged(dish, availableBefore, stockQuantityBefore);

    return DishResponse.from(dish);
  }

  @Transactional
  public DishResponse updateDishStatus(Long dishId, DishStatusRequest request) {

    Dish dish = dishRepository.findWithLockByIdAndIsDeletedFalse(dishId);

    boolean availableBefore = dish.isAvailable();
    Long stockQuantityBefore = dish.getStockQuantity();

    dish.updateStatus(request.dishStatus());

    appendStateEventIfChanged(dish, availableBefore, stockQuantityBefore);

    return DishResponse.from(dish);
  }

  private void validateDiscountRate(BigDecimal dishPrice, BigDecimal discountPrice) {
    BigDecimal discountRate =
        dishPrice.subtract(discountPrice).divide(dishPrice, 4, RoundingMode.HALF_UP);

    if (discountRate.compareTo(BigDecimal.valueOf(0.3)) < 0) {
      throw new BusinessException(ErrorCode.DISH_INVALID_DISCOUNT_RATE);
    }
  }

  @Transactional
  public void decreaseStock(Long dishId, Long quantity) {
    Dish dish = dishRepository.findWithLockByIdAndIsDeletedFalse(dishId);

    boolean availableBefore = dish.isAvailable();
    Long stockQuantityBefore = dish.getStockQuantity();

    dish.decreaseStock(quantity);

    appendStateEventIfChanged(dish, availableBefore, stockQuantityBefore);
  }

  @Transactional
  public void increaseStock(Long dishId, Long quantity) {

    Dish dish = dishRepository.findWithLockByIdAndIsDeletedFalse(dishId);

    boolean availableBefore = dish.isAvailable();

    Long stockQuantityBefore = dish.getStockQuantity();

    dish.increaseStock(quantity);

    appendStateEventIfChanged(dish, availableBefore, stockQuantityBefore);
  }

  @Transactional
  public void deleteDish(Long dishId) {
    Dish dish = dishRepository.findWithLockByIdAndIsDeletedFalse(dishId);

    boolean availableBefore = dish.isAvailable();
    Long stockQuantityBefore = dish.getStockQuantity();

    dish.delete();

    appendStateEventIfChanged(dish, availableBefore, stockQuantityBefore);
  }

  public DishResponse getEachDish(Long dishId) {
    Dish dish = getDish(dishId);
    return DishResponse.from(dish);
  }

  private Dish getDish(Long dishId) {
    return dishRepository.findByIdAndIsDeletedFalse(dishId);
  }

  /**
   * Cart 주문 가능 여부에 영향을 주는 Dish 상태가 바뀌었을 때 Outbox 이벤트를 기록합니다.
   *
   * <p>판매 가능 여부 또는 재고 중 하나라도 변경되면 이벤트를 생성합니다. 재고가 10개에서 5개로 감소해도 수량 7개가 담긴 CartItem은 주문 불가가 되므로
   * 이벤트가 필요합니다.
   *
   * @param dish 변경이 완료된 Dish
   * @param availableBefore 변경 전 판매 가능 여부
   * @param stockQuantityBefore 변경 전 재고
   */
  private void appendStateEventIfChanged(
      Dish dish, boolean availableBefore, Long stockQuantityBefore) {

    boolean availableAfter = dish.isAvailable();
    Long stockQuantityAfter = dish.getStockQuantity();
    boolean availabilityChanged = availableBefore != availableAfter;

    boolean stockQuantityChanged = !Objects.equals(stockQuantityBefore, stockQuantityAfter);

    if (!availabilityChanged && !stockQuantityChanged) {
      return;
    }

    long aggregateVersion = dish.nextEventVersion();

    DishStateChangedEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(),
            DishStateChangedEvent.SCHEMA_VERSION,
            dish.getId(),
            aggregateVersion,
            availableAfter,
            stockQuantityAfter,
            Instant.now());

    outboxEventWriter.append(event);
  }
}
