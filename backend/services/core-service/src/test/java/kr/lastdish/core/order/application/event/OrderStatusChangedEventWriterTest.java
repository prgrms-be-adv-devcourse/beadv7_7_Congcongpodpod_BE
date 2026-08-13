package kr.lastdish.core.order.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import kr.lastdish.common.event.DomainEvent;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.domain.event.OrderStatusChangedEvent;
import kr.lastdish.core.order.domain.event.OrderStatusChangedPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderStatusChangedEventWriterTest {

  @Test
  @DisplayName("주문 상태가 변경되면 주문 정보와 순서를 포함한 Outbox 이벤트를 생성한다")
  void append_createsOrderStatusChangedOutboxEvent() {
    // given: 픽업 준비 상태로 변경된 주문과 다음 이벤트 버전을 준비한다.
    OutboxEventWriter outboxEventWriter = mock(OutboxEventWriter.class);
    OrderStatusChangedEventWriter writer = new OrderStatusChangedEventWriter(outboxEventWriter);
    Order order = mock(Order.class);
    when(order.getId()).thenReturn(10L);
    when(order.getMemberId()).thenReturn(20L);
    when(order.getStatus()).thenReturn(OrderStatus.PICKUP_READY);
    when(order.nextEventVersion()).thenReturn(2L);

    // when: 주문 상태 변경 이벤트를 Outbox에 추가한다.
    writer.append(order);

    // then: 주문 식별자, 이벤트 순서, 회원 및 상태가 이벤트에 정확히 담겨야 한다.
    ArgumentCaptor<DomainEvent<?>> captor = ArgumentCaptor.forClass(DomainEvent.class);
    verify(outboxEventWriter).append(captor.capture());
    OrderStatusChangedEvent event = (OrderStatusChangedEvent) captor.getValue();
    assertThat(event.eventType()).isEqualTo("ORDER_STATUS_CHANGED");
    assertThat(event.aggregateId()).isEqualTo(10L);
    assertThat(event.aggregateVersion()).isEqualTo(2L);
    assertThat(event.payload())
        .isEqualTo(new OrderStatusChangedPayload(20L, OrderStatus.PICKUP_READY));
  }
}
