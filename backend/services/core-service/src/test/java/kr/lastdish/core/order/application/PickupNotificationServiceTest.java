package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.order.application.event.OrderNotificationEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PickupNotificationServiceTest {

  private OrderRepository orderRepository;
  private OrderNotificationEventWriter notificationEventWriter;
  private PickupNotificationService service;

  @BeforeEach
  void setUp() {
    orderRepository = mock(OrderRepository.class);
    notificationEventWriter = mock(OrderNotificationEventWriter.class);
    service = new PickupNotificationService(orderRepository, notificationEventWriter);
  }

  @Test
  void 현재_분의_픽업_시작_주문에_알림을_발행한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 26, 18, 0, 27);
    Order order = mock(Order.class);
    when(orderRepository.findPickupStartNotificationTargets(
            LocalTime.of(18, 0),
            LocalDateTime.of(2026, 8, 26, 18, 0),
            LocalDateTime.of(2026, 8, 27, 18, 0)))
        .thenReturn(List.of(order));

    assertThat(service.notifyDueOrders(now)).isEqualTo(1);

    verify(notificationEventWriter).appendPickupStarted(order);
  }

  @Test
  void 현재_분에서_15분_뒤_마감되는_주문에_알림을_발행한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 26, 18, 0, 27);
    Order order = mock(Order.class);
    when(orderRepository.findPickupDeadlineSoonNotificationTargets(
            LocalDateTime.of(2026, 8, 26, 18, 15), LocalDateTime.of(2026, 8, 26, 18, 16)))
        .thenReturn(List.of(order));

    assertThat(service.notifyDueOrders(now)).isEqualTo(1);

    verify(notificationEventWriter).appendPickupDeadlineSoon(order);
  }

  @Test
  void 시작과_마감_임박_대상을_각각_발행하고_합계를_반환한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 26, 18, 0);
    Order startOrder = mock(Order.class);
    Order deadlineOrder = mock(Order.class);
    when(orderRepository.findPickupStartNotificationTargets(
            LocalTime.of(18, 0), now, now.plusDays(1)))
        .thenReturn(List.of(startOrder));
    when(orderRepository.findPickupDeadlineSoonNotificationTargets(
            LocalDateTime.of(2026, 8, 26, 18, 15), LocalDateTime.of(2026, 8, 26, 18, 16)))
        .thenReturn(List.of(deadlineOrder));

    assertThat(service.notifyDueOrders(now)).isEqualTo(2);

    verify(notificationEventWriter).appendPickupStarted(startOrder);
    verify(notificationEventWriter).appendPickupDeadlineSoon(deadlineOrder);
  }
}
