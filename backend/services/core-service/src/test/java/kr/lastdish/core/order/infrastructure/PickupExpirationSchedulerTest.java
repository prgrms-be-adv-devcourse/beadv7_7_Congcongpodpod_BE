package kr.lastdish.core.order.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import kr.lastdish.core.order.application.OrderService;
import org.junit.jupiter.api.Test;

class PickupExpirationSchedulerTest {

  private final OrderService orderService = mock(OrderService.class);
  private final PickupExpirationScheduler scheduler = new PickupExpirationScheduler(orderService);

  @Test
  void 픽업_만료_처리를_OrderService에_위임한다() {
    scheduler.expirePickupOrders();

    verify(orderService).expirePickupOrders(any());
  }
}
