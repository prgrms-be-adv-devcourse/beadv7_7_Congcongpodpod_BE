package kr.lastdish.core.dish.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.event.DomainEvent;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.domain.DishStatus;
import kr.lastdish.core.dish.domain.event.DishCreatedEvent;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import kr.lastdish.core.dish.domain.event.DishUpdatedEvent;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import kr.lastdish.core.dish.presentation.dto.DishStatusRequest;
import kr.lastdish.core.dish.presentation.dto.DishUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DishServiceTest {

  @Mock private DishRepository dishRepository;

  @Mock private OutboxEventWriter outboxEventWriter;

  private DishService dishService;

  @BeforeEach
  void setUp() {
    dishService = new DishService(dishRepository, outboxEventWriter);
  }

  @Test
  void 상품_정보를_수정해도_기존_재고를_유지한다() {
    // given
    Dish dish = createDish(1L);

    /*
     * Dish ID는 JPA가 저장할 때 생성합니다.
     * 이 테스트에서는 저장 과정을 거치지 않으므로 ReflectionTestUtils로
     * 기존에 저장된 Dish처럼 ID를 설정합니다.
     */
    ReflectionTestUtils.setField(dish, "id", 10L);

    when(dishRepository.findWithLockByIdAndIsDeletedFalse(10L)).thenReturn(dish);

    DishUpdateRequest request = createUpdateRequest();

    // when
    DishResponse response = dishService.updateDish(10L, request);

    // then
    ArgumentCaptor<Dish> dishCaptor = ArgumentCaptor.forClass(Dish.class);
    verify(dishRepository).save(dishCaptor.capture());

    Dish replacement = dishCaptor.getValue();
    assertThat(replacement).isNotSameAs(dish);
    assertThat(dish.getIsDeleted()).isTrue();
    assertThat(replacement.getIsDeleted()).isFalse();
    assertThat(replacement.getStoreId()).isEqualTo(dish.getStoreId());
    assertThat(replacement.getStockQuantity()).isEqualTo(1L);
    assertThat(response.stockQuantity()).isEqualTo(1L);
    verify(outboxEventWriter, never()).append(any(DishStateChangedEvent.class));
    verify(outboxEventWriter).append(any(DishCreatedEvent.class));
  }

  @Test
  void 상품_정보_수정은_Dish_상태_이벤트를_기록하지_않는다() {
    // given
    Dish dish = createDish(10L);
    ReflectionTestUtils.setField(dish, "id", 10L);

    when(dishRepository.findWithLockByIdAndIsDeletedFalse(10L)).thenReturn(dish);

    DishUpdateRequest request = createUpdateRequest();

    // when
    DishResponse response = dishService.updateDish(10L, request);

    // then
    assertThat(response.stockQuantity()).isEqualTo(10L);
    verify(outboxEventWriter, never()).append(any(DishStateChangedEvent.class));
    verify(outboxEventWriter).append(any(DishCreatedEvent.class));
  }

  @Test
  void 상품_정보를_수정하면_새_Dish에_변경값을_반영한다() {
    // given
    Dish dish = createDish(10L);
    ReflectionTestUtils.setField(dish, "id", 10L);

    when(dishRepository.findWithLockByIdAndIsDeletedFalse(10L)).thenReturn(dish);

    DishUpdateRequest request =
        new DishUpdateRequest(
            10L,
            "김치찌개",
            LocalDateTime.now(),
            "상품 설명",
            BigDecimal.valueOf(10_000),
            BigDecimal.valueOf(7_000),
            LocalTime.of(18, 0),
            LocalTime.of(19, 0));

    // when
    dishService.updateDish(10L, request);

    // then
    ArgumentCaptor<Dish> dishCaptor = ArgumentCaptor.forClass(Dish.class);
    verify(dishRepository).save(dishCaptor.capture());
    assertThat(dishCaptor.getValue().getDiscountPrice()).isEqualByComparingTo("7000");
    verify(outboxEventWriter).append(any(DishCreatedEvent.class));
  }

  @Test
  void records_event_when_available_dish_is_deleted() {
    // given
    Dish dish = createDish(10L);
    ReflectionTestUtils.setField(dish, "id", 10L);

    when(dishRepository.findWithLockByIdAndIsDeletedFalse(10L)).thenReturn(dish);

    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    // when
    String imageKey = dishService.deleteDish(10L);

    // then
    List<DomainEvent> events = captureEvents(eventCaptor);
    DishStateChangedEvent event = findEvent(events, DishStateChangedEvent.class);

    assertThat(event.dishId()).isEqualTo(10L);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
    assertThat(event.payload().available()).isFalse();
    assertThat(imageKey).isEqualTo("dish/1/test.jpg");
  }

  private Dish createDish(Long stockQuantity) {
    return Dish.create(
        1L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        "한식",
        "dish/1/test.jpg",
        stockQuantity,
        BigDecimal.valueOf(10000),
        BigDecimal.ZERO,
        LocalTime.of(18, 0),
        LocalTime.of(19, 0));
  }

  private DishUpdateRequest createUpdateRequest() {
    return new DishUpdateRequest(
        10L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        BigDecimal.valueOf(10000),
        BigDecimal.ZERO,
        LocalTime.of(18, 0),
        LocalTime.of(19, 0));
  }

  @Test
  void 재고가_감소하면_Dish_상태_이벤트를_기록한다() {
    // given
    Dish dish = createDish(10L);

    given(dishRepository.findWithLockByIdAndIsDeletedFalse(1L)).willReturn(dish);

    // when
    dishService.decreaseStock(1L, 5L);

    // then
    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    List<DomainEvent> events = captureEvents(eventCaptor);
    DishStateChangedEvent event = findEvent(events, DishStateChangedEvent.class);

    assertThat(event.payload().available()).isTrue();
    assertThat(event.payload().stockQuantity()).isEqualTo(5L);
    assertThat(events).anyMatch(DishUpdatedEvent.class::isInstance);
  }

  @Test
  void 재고가_증가하면_Dish_수정_이벤트를_기록한다() {
    Dish dish = createDish(5L);
    ReflectionTestUtils.setField(dish, "id", 10L);
    given(dishRepository.findWithLockByIdAndIsDeletedFalse(10L)).willReturn(dish);
    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    dishService.increaseStock(10L, 5L);

    List<DomainEvent> events = captureEvents(eventCaptor);
    assertThat(events).anyMatch(DishStateChangedEvent.class::isInstance);
    assertThat(events).anyMatch(DishUpdatedEvent.class::isInstance);
  }

  @Test
  void 판매_상태가_변경되면_Dish_수정_이벤트를_기록한다() {
    Dish dish = createDish(10L);
    ReflectionTestUtils.setField(dish, "id", 10L);
    given(dishRepository.findWithLockByIdAndIsDeletedFalse(10L)).willReturn(dish);
    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    dishService.updateDishStatus(10L, new DishStatusRequest(DishStatus.SOLD_OUT));

    List<DomainEvent> events = captureEvents(eventCaptor);
    assertThat(events).anyMatch(DishStateChangedEvent.class::isInstance);
    assertThat(events).anyMatch(DishUpdatedEvent.class::isInstance);
  }

  @Test
  void 매장_마감으로_판매를_종료하면_재고를_0으로_초기화하고_상태_이벤트를_기록한다() {
    Dish dish = createDish(10L);
    ReflectionTestUtils.setField(dish, "id", 10L);
    when(dishRepository.findWithLockByStoreIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(dish));
    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    dishService.closeSaleByStoreId(1L);

    assertThat(dish.getStockQuantity()).isZero();
    assertThat(dish.getDishStatus()).isEqualTo(kr.lastdish.core.dish.domain.DishStatus.SOLD_OUT);
    List<DomainEvent> events = captureEvents(eventCaptor);
    DishStateChangedEvent event = findEvent(events, DishStateChangedEvent.class);
    assertThat(event.payload().available()).isFalse();
    assertThat(event.payload().stockQuantity()).isZero();
    assertThat(events).anyMatch(DishUpdatedEvent.class::isInstance);
  }

  private List<DomainEvent> captureEvents(ArgumentCaptor<DomainEvent> eventCaptor) {
    verify(outboxEventWriter, atLeastOnce()).append(eventCaptor.capture());
    return eventCaptor.getAllValues();
  }

  private <T extends DomainEvent> T findEvent(List<DomainEvent> events, Class<T> eventType) {
    return events.stream()
        .filter(eventType::isInstance)
        .map(eventType::cast)
        .findFirst()
        .orElseThrow(() -> new AssertionError("발행되지 않은 이벤트: " + eventType.getSimpleName()));
  }

  @Test
  void 재고_조정_delta가_양수면_재고를_늘린다() {
    Dish dish = createDish(10L);
    given(dishRepository.findWithLockByIdAndIsDeletedFalse(1L)).willReturn(dish);

    DishResponse response = dishService.adjustStock(1L, 5L);

    assertThat(response.stockQuantity()).isEqualTo(15L);
    then(outboxEventWriter).should().append(any());
  }

  @Test
  void 재고_조정_delta가_음수면_재고를_줄인다() {
    Dish dish = createDish(10L);
    given(dishRepository.findWithLockByIdAndIsDeletedFalse(1L)).willReturn(dish);

    DishResponse response = dishService.adjustStock(1L, -4L);

    assertThat(response.stockQuantity()).isEqualTo(6L);
    then(outboxEventWriter).should().append(any());
  }

  @Test
  void 재고_조정_delta가_0이면_예외를_던진다() {
    assertThatThrownBy(() -> dishService.adjustStock(1L, 0L))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> {
              assertThat(exception.getErrorCode().getCode()).isEqualTo("D008");
              assertThat(exception.getMessage()).isEqualTo("재고 변경량은 0이 아니어야 합니다.");
            });

    then(dishRepository).shouldHaveNoInteractions();
    then(outboxEventWriter).shouldHaveNoInteractions();
  }
}
