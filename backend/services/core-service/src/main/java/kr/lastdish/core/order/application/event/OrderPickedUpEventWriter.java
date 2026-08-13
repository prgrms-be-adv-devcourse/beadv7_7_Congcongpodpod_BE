package kr.lastdish.core.order.application.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.event.OrderPickedUpEvent;
import kr.lastdish.core.order.domain.event.OrderPickedUpPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPickedUpEventWriter {

  private final OutboxEventWriter outboxEventWriter;

  public void append(Order order) {
    OrderPickedUpEvent event =
        new OrderPickedUpEvent(
            UUID.randomUUID(),
            OrderPickedUpEvent.SCHEMA_VERSION,
            order.getId(),
            order.nextEventVersion(),
            new OrderPickedUpPayload(
                order.getMemberId(), order.getStoreId(), order.getTotalPrice()),
            Instant.now());

    outboxEventWriter.append(event);
  }
}
