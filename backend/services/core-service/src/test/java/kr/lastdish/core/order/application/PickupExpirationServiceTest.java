package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import org.junit.jupiter.api.Test;

class PickupExpirationServiceTest {

  private final OrderRepository orderRepository = mock(OrderRepository.class);
  private final OrderStatusChangedEventWriter orderStatusChangedEventWriter =
      mock(OrderStatusChangedEventWriter.class);
  private final PickupExpirationService pickupExpirationService =
      new PickupExpirationService(orderRepository, orderStatusChangedEventWriter);

  @Test
  void expiresPickupReadyOrdersPastTheirDeadline() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 19, 0);
    Order firstOrder = mock(Order.class);
    Order secondOrder = mock(Order.class);

    when(orderRepository.findPickupExpirationTargets(now))
        .thenReturn(List.of(firstOrder, secondOrder));

    int expiredCount = pickupExpirationService.expire(now);

    assertThat(expiredCount).isEqualTo(2);
    verify(firstOrder).markNoShow();
    verify(secondOrder).markNoShow();
    verify(orderStatusChangedEventWriter).append(firstOrder);
    verify(orderStatusChangedEventWriter).append(secondOrder);
  }
}
