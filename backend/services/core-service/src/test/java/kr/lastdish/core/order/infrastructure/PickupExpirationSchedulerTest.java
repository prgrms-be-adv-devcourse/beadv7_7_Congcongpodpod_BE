package kr.lastdish.core.order.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.lastdish.core.order.application.OrderService;
import org.junit.jupiter.api.Test;

class PickupExpirationSchedulerTest {

  private final OrderService orderService = mock(OrderService.class);
  private final PickupExpirationScheduler scheduler = new PickupExpirationScheduler(orderService);

  @Test
  void 픽업_만료_대상이_한_배치를_채우면_다음_배치를_계속_처리한다() {
    when(orderService.expirePickupOrders(any())).thenReturn(1000, 20);

    int expiredCount = scheduler.expirePickupOrders();

    assertThat(expiredCount).isEqualTo(1020);
    verify(orderService, times(2)).expirePickupOrders(any());
  }
}
