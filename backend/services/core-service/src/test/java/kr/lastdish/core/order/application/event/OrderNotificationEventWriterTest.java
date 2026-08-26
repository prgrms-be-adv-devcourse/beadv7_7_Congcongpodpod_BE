package kr.lastdish.core.order.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.lastdish.common.event.DomainEvent;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRejectReason;
import kr.lastdish.core.order.domain.event.OrderNotificationEvent;
import kr.lastdish.core.order.domain.event.OrderNotificationPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderNotificationEventWriterTest {

  private OutboxEventWriter outboxEventWriter;
  private OrderNotificationEventWriter writer;
  private Order order;

  @BeforeEach
  void setUp() {
    outboxEventWriter = mock(OutboxEventWriter.class);
    writer = new OrderNotificationEventWriter(outboxEventWriter);
    order = mock(Order.class);
    when(order.getId()).thenReturn(10L);
    when(order.getMemberId()).thenReturn(1L);
    when(order.nextEventVersion()).thenReturn(2L);
  }

  @Test
  void appendCancelled_createsNotificationForSeller() {
    when(order.getDishName()).thenReturn("마감 할인 도시락");

    writer.appendCancelled(order, 20L);

    assertEvent(
        new OrderNotificationPayload(
            20L,
            "ORDER_CANCELLED",
            "주문이 취소됐어요",
            "마감 할인 도시락 주문이 주문자에 의해 취소되었습니다.",
            null,
            null,
            null));
  }

  @Test
  void appendAccepted_createsNotificationForCustomer() {
    writer.appendAccepted(order);

    assertEvent(
        new OrderNotificationPayload(
            1L, "ORDER_ACCEPTED", "주문이 접수됐어요", "매장에서 주문을 확인하고 상품을 준비하고 있어요.", null, "ORDER", 10L));
  }

  @Test
  void appendRejected_createsNotificationForCustomer() {
    OrderRejectReason reason = OrderRejectReason.OUT_OF_STOCK;

    writer.appendRejected(order, reason);

    assertEvent(
        new OrderNotificationPayload(
            1L, "ORDER_REJECTED", "주문이 반려됐어요", reason.getMessage(), null, "ORDER", 10L));
  }

  private void assertEvent(OrderNotificationPayload expectedPayload) {
    ArgumentCaptor<DomainEvent<?>> captor = ArgumentCaptor.forClass(DomainEvent.class);
    verify(outboxEventWriter).append(captor.capture());

    OrderNotificationEvent event = (OrderNotificationEvent) captor.getValue();
    assertThat(event.eventType()).isEqualTo(OrderNotificationEvent.EVENT_TYPE);
    assertThat(event.aggregateType()).isEqualTo("ORDER");
    assertThat(event.aggregateId()).isEqualTo(10L);
    assertThat(event.aggregateVersion()).isEqualTo(2L);
    assertThat(event.schemaVersion()).isEqualTo(OrderNotificationEvent.SCHEMA_VERSION);
    assertThat(event.eventId()).isNotNull();
    assertThat(event.occurredAt()).isNotNull();
    assertThat(event.payload()).isEqualTo(expectedPayload);
  }
}
