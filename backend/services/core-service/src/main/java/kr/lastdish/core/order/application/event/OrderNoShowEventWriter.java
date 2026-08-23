package kr.lastdish.core.order.application.event;

import java.time.ZoneId;
import java.util.UUID;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.event.OrderNoShowEvent;
import kr.lastdish.core.order.domain.event.OrderNoShowPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderNoShowEventWriter {

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

  private final OutboxEventWriter outboxEventWriter;

  public void append(Order order, long aggregateVersion) {
    OrderNoShowEvent event =
        new OrderNoShowEvent(
            UUID.randomUUID(),
            OrderNoShowEvent.SCHEMA_VERSION,
            order.getId(),
            aggregateVersion,
            new OrderNoShowPayload(
                order.getId(),
                order.getStoreId(),
                order.getTotalPrice(),
                order.getPickupResultAt()),
            order.getPickupResultAt().atZone(BUSINESS_ZONE).toInstant());

    outboxEventWriter.append(event);
  }
}
