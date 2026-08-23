package kr.lastdish.core.order.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import kr.lastdish.common.event.DomainEvent;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.event.OrderNoShowEvent;
import kr.lastdish.core.order.domain.event.OrderNoShowPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderNoShowEventWriterTest {

  @Test
  @DisplayName("노쇼 처리 시 정산에 필요한 Outbox 이벤트를 생성한다")
  void append_createsOrderNoShowOutboxEvent() {
    OutboxEventWriter outboxEventWriter = mock(OutboxEventWriter.class);
    OrderNoShowEventWriter writer = new OrderNoShowEventWriter(outboxEventWriter);
    Order order = mock(Order.class);
    when(order.getId()).thenReturn(10L);
    when(order.getStoreId()).thenReturn(30L);
    when(order.getTotalPrice()).thenReturn(new BigDecimal("12000"));
    when(order.getPickupResultAt()).thenReturn(LocalDateTime.of(2026, 8, 13, 19, 0));

    writer.append(order, 4L);

    ArgumentCaptor<DomainEvent<?>> captor = ArgumentCaptor.forClass(DomainEvent.class);
    verify(outboxEventWriter).append(captor.capture());

    OrderNoShowEvent event = (OrderNoShowEvent) captor.getValue();
    assertThat(event.eventType()).isEqualTo(OrderNoShowEvent.EVENT_TYPE);
    assertThat(event.aggregateType()).isEqualTo("ORDER");
    assertThat(event.aggregateId()).isEqualTo(10L);
    assertThat(event.aggregateVersion()).isEqualTo(4L);
    assertThat(event.schemaVersion()).isEqualTo(OrderNoShowEvent.SCHEMA_VERSION);
    assertThat(event.eventId()).isNotNull();
    assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-08-13T10:00:00Z"));
    assertThat(event.payload()).isEqualTo(new OrderNoShowPayload(10L, 30L, new BigDecimal("12000"), LocalDateTime.of(2026, 8, 13, 19, 0)));
  }
}
