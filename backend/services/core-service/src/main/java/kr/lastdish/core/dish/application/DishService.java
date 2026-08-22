package kr.lastdish.core.dish.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.dish.application.dto.InternalDishResult;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.domain.event.*;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.dish.presentation.dto.DishStatusRequest;
import kr.lastdish.core.dish.presentation.dto.DishUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DishService {
  private final DishRepository dishRepository;
  private final OutboxEventWriter outboxEventWriter;

  @Transactional
  public DishResponse createDish(DishCreateRequest request, String finalImageKey) {
    Dish dish =
        Dish.create(
            request.storeId(),
            request.dishName(),
            request.registeredAt(),
            request.description(),
            request.category(),
            finalImageKey,
            request.stockQuantity(),
            request.dishPrice(),
            request.discountPrice(),
            request.pickupStartTime(),
            request.pickupEndTime());

    Dish savedDish = dishRepository.save(dish);

    //    TODO : 리스너 구현 시 이벤트 발행 활성화
    //    appendCreatedEvent(savedDish);

    return DishResponse.from(savedDish);
  }

  public void validateCreateDish(DishCreateRequest request) {
    validateDiscountRate(request.dishPrice(), request.discountPrice());
    if (dishRepository.existsByStoreIdAndIsDeletedFalse(request.storeId())) {
      throw new BusinessException(ErrorCode.DISH_ALREADY_EXISTS);
    }
  }

  @Transactional
  public DishResponse updateDish(Long dishId, DishUpdateRequest request) {
    // 할인율 검증
    validateDiscountRate(request.dishPrice(), request.discountPrice());

    // 동시에 같은 Dish가 변경되면 동일한 event Version이 생성되지 않도록 이벤트가 발생하는 변경 메서드는 잠금 조회를 사용합니다.
    Dish dish = dishRepository.findWithLockByIdAndIsDeletedFalse(dishId);
    // Dish 변경 전 가격을 저장합니다. Cart가 절약 금액 산출에 정가도 쓰므로 정가 변경도 함께 본다.
    BigDecimal dishPriceBefore = dish.getDishPrice();
    BigDecimal unitPriceBefore = dish.getDiscountPrice();

    dish.update(
        request.dishName(),
        request.registeredAt(),
        request.description(),
        request.dishPrice(),
        request.discountPrice(),
        request.pickupStartTime(),
        request.pickupEndTime());

    appendPriceEventIfChanged(dish, dishPriceBefore, unitPriceBefore);

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

  /**
   * 판매자가 재고를 상대값(delta)으로 조정합니다.
   *
   * <p>Order 흐름의 {@link #increaseStock}/{@link #decreaseStock}과 달리, 판매자가 직접 입력한 증감분을 부호로 판단해 그중 하나로
   * 위임합니다.
   */
  @Transactional
  public DishResponse adjustStock(Long dishId, Long quantityDelta) {
    if (quantityDelta == null || quantityDelta == 0) {
      throw new BusinessException(ErrorCode.INVALID_STOCK_DELTA);
    }

    Dish dish = dishRepository.findWithLockByIdAndIsDeletedFalse(dishId);

    boolean availableBefore = dish.isAvailable();
    Long stockQuantityBefore = dish.getStockQuantity();

    if (quantityDelta > 0) {
      dish.increaseStock(quantityDelta);
    } else {
      dish.decreaseStock(-quantityDelta);
    }

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
  public void closeSaleByStoreId(Long storeId) {
    dishRepository
        .findWithLockByStoreIdAndIsDeletedFalse(storeId)
        .ifPresent(
            dish -> {
              boolean availableBefore = dish.isAvailable();
              Long stockQuantityBefore = dish.getStockQuantity();
              dish.closeSale();
              appendStateEventIfChanged(dish, availableBefore, stockQuantityBefore);
            });
  }

  @Transactional
  public String deleteDish(Long dishId) {
    Dish dish = dishRepository.findWithLockByIdAndIsDeletedFalse(dishId);

    boolean availableBefore = dish.isAvailable();
    Long stockQuantityBefore = dish.getStockQuantity();

    dish.delete();

    appendStateEventIfChanged(dish, availableBefore, stockQuantityBefore);
    return dish.getThumbnailUrl();
  }

  public DishResponse getEachDish(Long dishId) {
    Dish dish = getDish(dishId);
    return DishResponse.from(dish);
  }

  public String getImageKey(Long dishId) {
    String imageKey = getDish(dishId).getThumbnailUrl();
    if (imageKey == null || imageKey.isBlank()) {
      throw new BusinessException(ErrorCode.IMAGE_OBJECT_NOT_FOUND);
    }
    return imageKey;
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

    DishStateChangedPayload payload =
        new DishStateChangedPayload(availableAfter, stockQuantityAfter);

    boolean availabilityChanged = availableBefore != availableAfter;

    boolean stockQuantityChanged = !Objects.equals(stockQuantityBefore, stockQuantityAfter);

    if (!availabilityChanged && !stockQuantityChanged) {
      return;
    }

    long aggregateVersion = dish.nextAggregateVersion();

    DishStateChangedEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(),
            DishStateChangedEvent.SCHEMA_VERSION,
            dish.getId(),
            aggregateVersion,
            payload,
            Instant.now());

    outboxEventWriter.append(event);
  }

  /**
   * Cart의 가격 표시에 영향을 주는 Dish 상태가 바뀌었을 때 Outbox 이벤트를 기록합니다.
   *
   * <p>정가나 판매가 중 하나라도 변경되면 해당 이벤트를 발행 합니다. Cart가 절약 금액(정가 - 판매가) 산출에 두 값을 모두 쓰기 때문에 판매가만 보고 판단하면
   * 정가만 바뀐 변경을 놓친다.
   *
   * @param dish 변경이 완료된 Dish
   * @param dishPriceBefore 변경 전 정가
   * @param unitPriceBefore 변경 전 판매가
   */
  private void appendPriceEventIfChanged(
      Dish dish, BigDecimal dishPriceBefore, BigDecimal unitPriceBefore) {

    BigDecimal dishPriceAfter = dish.getDishPrice();
    BigDecimal unitPriceAfter = dish.getDiscountPrice();

    if (dishPriceBefore.compareTo(dishPriceAfter) == 0
        && unitPriceBefore.compareTo(unitPriceAfter) == 0) {
      return;
    }

    long aggregateVersion = dish.nextAggregateVersion();

    DishPriceChangedEvent event =
        new DishPriceChangedEvent(
            UUID.randomUUID(),
            DishPriceChangedEvent.SCHEMA_VERSION,
            dish.getId(),
            aggregateVersion,
            new DishPriceChangedPayload(dishPriceAfter, unitPriceAfter),
            Instant.now());

    outboxEventWriter.append(event);
  }

  // 검색 문서 갱신을 요청하는 상품 생성 이벤트를 Outbox에 기록한다.
  private void appendCreatedEvent(Dish dish) {
    long aggregateVersion = dish.nextAggregateVersion();
    DishCreatedPayload payload = new DishCreatedPayload(dish.getStoreId());

    DishCreatedEvent event =
        new DishCreatedEvent(
            UUID.randomUUID(),
            DishCreatedEvent.SCHEMA_VERSION,
            dish.getId(),
            aggregateVersion,
            payload,
            Instant.now());

    outboxEventWriter.append(event);
  }

  public List<DishResponse> getOnSaleDishesByStoreId(Long storeId) {
    return dishRepository.findOnSaleByStoreId(storeId).stream().map(DishResponse::from).toList();
  }

  // Seller 상품관리용 조회 — 판매 상태와 무관하게 매장의 상품을 반환한다.
  public DishResponse getDishByStoreId(Long storeId) {
    Dish dish =
        dishRepository
            .findByStoreIdAndIsDeletedFalse(storeId)
            .orElseThrow(() -> new BusinessException(ErrorCode.DISH_NOT_FOUND));

    return DishResponse.from(dish);
  }

  // 검색 색인 재생성용 조회 — 상품 미등록 매장은 비어 있는 Optional을 반환한다.
  public Optional<InternalDishResult> getDishByStoreIdForRenewal(Long storeId) {
    return dishRepository.findByStoreIdAndIsDeletedFalse(storeId).map(InternalDishResult::from);
  }
}
