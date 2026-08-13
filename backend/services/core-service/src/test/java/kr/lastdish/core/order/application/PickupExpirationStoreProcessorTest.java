package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.core.dish.application.DishService;
import kr.lastdish.core.order.application.event.OrderNoShowEventWriter;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.store.application.StoreFacade;
import org.junit.jupiter.api.Test;

class PickupExpirationStoreProcessorTest {

  private final OrderRepository orderRepository = mock(OrderRepository.class);
  private final OrderStatusChangedEventWriter eventWriter =
      mock(OrderStatusChangedEventWriter.class);
  private final OrderNoShowEventWriter noShowEventWriter = mock(OrderNoShowEventWriter.class);
  private final StoreFacade storeFacade = mock(StoreFacade.class);
  private final DishService dishService = mock(DishService.class);
  private final PickupExpirationStoreProcessor processor =
      new PickupExpirationStoreProcessor(
          orderRepository, eventWriter, noShowEventWriter, storeFacade, dishService);

  @Test
  void 매장의_마감_대상_주문을_노쇼_처리한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 19, 0);
    Order firstOrder = mock(Order.class);
    Order secondOrder = mock(Order.class);
    when(orderRepository.findPickupExpirationTargets(1L, now))
        .thenReturn(List.of(firstOrder, secondOrder));
    when(firstOrder.nextEventVersion()).thenReturn(3L);
    when(secondOrder.nextEventVersion()).thenReturn(5L);

    assertThat(processor.expireStore(1L, now)).isEqualTo(2);
    verify(storeFacade).rescheduleNextClosingAt(1L, now);
    verify(firstOrder).markNoShow(now);
    verify(secondOrder).markNoShow(now);
    verify(eventWriter).append(firstOrder, 3L);
    verify(eventWriter).append(secondOrder, 5L);
    verifyNoInteractions(noShowEventWriter);
    verify(dishService).closeSaleByStoreId(1L);
  }
}
