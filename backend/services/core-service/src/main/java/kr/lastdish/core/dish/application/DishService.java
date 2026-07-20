package kr.lastdish.core.dish.application;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.common.event.dish.DishAvailabilityChangedEvent;
import kr.lastdish.core.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.presentation.dto.DIshUpdateRequest;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DishService {
  private final DishRepository dishRepository;
  private final OutboxEventWriter outboxEventWriter;

  @Transactional
  public DishResponse createDish(DishCreateRequest request) {
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
  public DishResponse updateDish(Long dishId, DIshUpdateRequest request) {
    Dish dish = getDish(dishId);

    /*
     * 엔티티를 변경하기 전에 기존 판매 가능 여부를 저장합니다.
     * Dish를 변경한 뒤에는 이전 상태를 알 수 없기 때문입니다.
     */
    boolean availableBefore = dish.isAvailable();

    dish.update(
        request.dishName(),
        request.registeredAt(),
        request.description(),
        request.category(),
        request.thumbnailUrl(),
        request.stockQuantity(),
        request.dishPrice(),
        request.discountPrice());

    /*
     * 변경 전후의 판매 가능 여부가 달라졌을 때만
     * Cart 등 다른 도메인에 전달할 이벤트를 기록합니다.
     */
    appendAvailabilityEventIfChanged(dish, availableBefore);

    return DishResponse.from(dish);
  }

  @Transactional
  public void deleteDish(Long dishId) {
    Dish dish = getDish(dishId);

    /*
     * 판매 중인 Dish가 삭제되면 판매 불가능 상태로 변경되므로
     * 삭제 전 상태를 저장합니다.
     */
    boolean availableBefore = dish.isAvailable();

    dish.delete();

    appendAvailabilityEventIfChanged(dish, availableBefore);
  }

  public DishResponse getEachDish(Long dishId) {
    Dish dish = getDish(dishId);
    return DishResponse.from(dish);
  }

  private Dish getDish(Long dishId) {
    return dishRepository.findByIdAndIsDeletedFalse(dishId);
  }

  /**
   * Dish의 판매 가능 여부가 실제로 변경된 경우에만 Outbox 이벤트를 기록합니다.
   *
   * <p>재고가 10개에서 5개로 바뀌면 여전히 판매 가능하므로 이벤트를 만들지 않습니다. 재고가 1개에서 0개가 되거나, 품절 상품의 재고가 다시 생기는 경우에만 판매 가능
   * 여부 변경 이벤트를 생성합니다.
   *
   * @param dish 변경이 완료된 Dish
   * @param availableBefore 변경 전 판매 가능 여부
   */
  private void appendAvailabilityEventIfChanged(Dish dish, boolean availableBefore) {
    boolean availableAfter = dish.isAvailable();

    /*
     * 판매 가능 여부가 동일하면 CartItem 상태를 변경할 필요가 없습니다.
     */
    if (availableBefore == availableAfter) {
      return;
    }

    DishAvailabilityChangedEvent event =
        new DishAvailabilityChangedEvent(
            UUID.randomUUID(),
            DishAvailabilityChangedEvent.SCHEMA_VERSION,
            dish.getId(),
            availableAfter,
            Instant.now());

    /*
     * OutboxEventWriter는 별도 트랜잭션을 열지 않습니다.
     * 따라서 현재 DishService 트랜잭션에 참여합니다.
     *
     * Outbox 저장이 실패하면 Dish 변경도 함께 롤백됩니다.
     */
    outboxEventWriter.append(event);
  }
}
