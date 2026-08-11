package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.core.dish.application.DishService;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.store.application.StoreFacade;
import org.junit.jupiter.api.Test;

class PickupExpirationServiceTest {

  private final OrderRepository orderRepository = mock(OrderRepository.class);
  private final OrderStatusChangedEventWriter orderStatusChangedEventWriter =
      mock(OrderStatusChangedEventWriter.class);
  private final StoreFacade storeFacade = mock(StoreFacade.class);
  private final DishService dishService = mock(DishService.class);
  private final PickupExpirationService pickupExpirationService =
      new PickupExpirationService(
          orderRepository, orderStatusChangedEventWriter, storeFacade, dishService);

  @Test
  void closesDishesAndMarksUnpickedOrdersAsNoShowWhenStoresClose() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 19, 0);
    List<Long> closingStoreIds = List.of(1L, 2L);
    Order firstOrder = mock(Order.class);
    Order secondOrder = mock(Order.class);

    when(storeFacade.claimStoresReadyToClose(now)).thenReturn(closingStoreIds);
    when(orderRepository.findPickupExpirationTargets(closingStoreIds))
        .thenReturn(List.of(firstOrder, secondOrder));

    int expiredCount = pickupExpirationService.expire(now);

    assertThat(expiredCount).isEqualTo(2);
    verify(firstOrder).markNoShow(now);
    verify(secondOrder).markNoShow(now);
    verify(orderStatusChangedEventWriter).append(firstOrder);
    verify(orderStatusChangedEventWriter).append(secondOrder);
    verify(dishService).closeSaleByStoreId(1L);
    verify(dishService).closeSaleByStoreId(2L);
  }
}
