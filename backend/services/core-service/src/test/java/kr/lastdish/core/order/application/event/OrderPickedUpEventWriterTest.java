package kr.lastdish.core.order.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import kr.lastdish.common.event.DomainEvent;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.event.OrderPickedUpEvent;
import kr.lastdish.core.order.domain.event.OrderPickedUpPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderPickedUpEventWriterTest {

  @Test
  @DisplayName("픽업 완료 시 정산과 포인트에 필요한 Outbox 이벤트를 생성한다")
  void append_createsOrderPickedUpOutboxEvent() {
    OutboxEventWriter outboxEventWriter = mock(OutboxEventWriter.class);
    OrderPickedUpEventWriter writer = new OrderPickedUpEventWriter(outboxEventWriter);
    Order order = mock(Order.class);
    when(order.getId()).thenReturn(10L);
    when(order.getMemberId()).thenReturn(20L);
    when(order.getStoreId()).thenReturn(30L);
    when(order.getTotalPrice()).thenReturn(new BigDecimal("12000"));
    when(order.nextEventVersion()).thenReturn(4L);

    writer.append(order);

    ArgumentCaptor<DomainEvent<?>> captor = ArgumentCaptor.forClass(DomainEvent.class);
    verify(outboxEventWriter).append(captor.capture());

    OrderPickedUpEvent event = (OrderPickedUpEvent) captor.getValue();
    assertThat(event.eventType()).isEqualTo(OrderPickedUpEvent.EVENT_TYPE);
    assertThat(event.aggregateType()).isEqualTo("ORDER");
    assertThat(event.aggregateId()).isEqualTo(10L);
    assertThat(event.aggregateVersion()).isEqualTo(4L);
    assertThat(event.schemaVersion()).isEqualTo(OrderPickedUpEvent.SCHEMA_VERSION);
    assertThat(event.eventId()).isNotNull();
    assertThat(event.occurredAt()).isNotNull();
    assertThat(event.payload())
        .isEqualTo(new OrderPickedUpPayload(20L, 30L, new BigDecimal("12000")));
  }
}
