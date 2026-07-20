package kr.lastdish.core.dish.application;

import kr.lastdish.core.common.event.DomainEvent;
import kr.lastdish.core.common.event.dish.DishAvailabilityChangedEvent;
import kr.lastdish.core.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.dish.domain.Category;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.presentation.dto.DIshUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
  void records_event_when_dish_becomes_unavailable() {
    // given
    Dish dish = createDish(1L);

    /*
     * Dish ID는 JPA가 저장할 때 생성합니다.
     * 이 테스트에서는 저장 과정을 거치지 않으므로 ReflectionTestUtils로
     * 기존에 저장된 Dish처럼 ID를 설정합니다.
     */
    ReflectionTestUtils.setField(dish, "id", 10L);

    when(dishRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(dish);

    DIshUpdateRequest request = createUpdateRequest(0L);

    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    // when
    dishService.updateDish(10L, request);

    // then
    verify(outboxEventWriter).append(eventCaptor.capture());

    DomainEvent capturedEvent = eventCaptor.getValue();

    assertThat(capturedEvent).isInstanceOf(DishAvailabilityChangedEvent.class);

    DishAvailabilityChangedEvent event = (DishAvailabilityChangedEvent) capturedEvent;

    assertThat(event.dishId()).isEqualTo(10L);
    assertThat(event.available()).isFalse();
    assertThat(event.schemaVersion()).isEqualTo(DishAvailabilityChangedEvent.SCHEMA_VERSION);
  }

  @Test
  void does_not_record_event_when_availability_is_unchanged() {
    // given
    Dish dish = createDish(10L);
    ReflectionTestUtils.setField(dish, "id", 10L);

    when(dishRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(dish);

    /*
     * 재고는 10개에서 5개로 바뀌지만 변경 전후 모두 판매 가능합니다.
     */
    DIshUpdateRequest request = createUpdateRequest(5L);

    // when
    dishService.updateDish(10L, request);

    // then
    verify(outboxEventWriter, never()).append(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void records_event_when_available_dish_is_deleted() {
    // given
    Dish dish = createDish(10L);
    ReflectionTestUtils.setField(dish, "id", 10L);

    when(dishRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(dish);

    ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);

    // when
    dishService.deleteDish(10L);

    // then
    verify(outboxEventWriter).append(eventCaptor.capture());

    DishAvailabilityChangedEvent event = (DishAvailabilityChangedEvent) eventCaptor.getValue();

    assertThat(event.dishId()).isEqualTo(10L);
    assertThat(event.available()).isFalse();
  }

  private Dish createDish(Long stockQuantity) {
    return Dish.create(
        1L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        Category.KOREAN,
        null,
        stockQuantity,
        BigDecimal.valueOf(10000),
        BigDecimal.ZERO);
  }

  private DIshUpdateRequest createUpdateRequest(Long stockQuantity) {
    return new DIshUpdateRequest(
        10L,
        "김치찌개",
        LocalDateTime.now(),
        "상품 설명",
        Category.KOREAN,
        null,
        stockQuantity,
        BigDecimal.valueOf(10000),
        BigDecimal.ZERO);
  }
}
