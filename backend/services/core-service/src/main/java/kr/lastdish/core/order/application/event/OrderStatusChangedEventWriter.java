package kr.lastdish.core.order.application.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.event.OrderStatusChangedEvent;
import kr.lastdish.core.order.domain.event.OrderStatusChangedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusChangedEventWriter {

  private final OutboxEventWriter outboxEventWriter;

  public void append(Order order) {
    append(order, order.nextEventVersion());
  }

  public void append(Order order, long aggregateVersion) {
    OrderStatusChangedEvent event =
        new OrderStatusChangedEvent(
            UUID.randomUUID(),
            OrderStatusChangedEvent.SCHEMA_VERSION,
            order.getId(),
            aggregateVersion,
            new OrderStatusChangedPayload(order.getMemberId(), order.getStatus()),
            Instant.now());

    outboxEventWriter.append(event);
  }
}
