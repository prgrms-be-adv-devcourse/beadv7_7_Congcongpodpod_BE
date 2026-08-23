package kr.lastdish.core.order.application.event;

import java.time.ZoneId;
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

  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

  private final OutboxEventWriter outboxEventWriter;

  public void append(Order order, long aggregateVersion) {
    OrderPickedUpEvent event =
        new OrderPickedUpEvent(
            UUID.randomUUID(),
            OrderPickedUpEvent.SCHEMA_VERSION,
            order.getId(),
            aggregateVersion,
            new OrderPickedUpPayload(
                order.getId(),
                order.getMemberId(),
                order.getStoreId(),
                order.getTotalPrice(),
                order.getTotalSavedAmount(),
                order.getPickupResultAt()),
            order.getPickupResultAt().atZone(BUSINESS_ZONE).toInstant());

    outboxEventWriter.append(event);
  }
}
