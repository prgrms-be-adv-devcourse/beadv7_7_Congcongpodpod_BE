package kr.lastdish.core.order.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record OrderStatusChangedEvent(
    UUID eventId,
    int schemaVersion,
    Long orderId,
    long aggregateVersion,
    OrderStatusChangedPayload payload,
    Instant occurredAt)
    implements DomainEvent<OrderStatusChangedPayload> {

  public static final String EVENT_TYPE = "ORDER_STATUS_CHANGED";
  public static final int SCHEMA_VERSION = 1;

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public String aggregateType() {
    return "ORDER";
  }

  @Override
  public Long aggregateId() {
    return orderId;
  }
}
