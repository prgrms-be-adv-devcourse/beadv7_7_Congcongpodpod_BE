package kr.lastdish.core.order.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record OrderNotificationEvent(
    UUID eventId,
    int schemaVersion,
    Long orderId,
    long aggregateVersion,
    OrderNotificationPayload payload,
    Instant occurredAt)
    implements DomainEvent<OrderNotificationPayload> {

  public static final String EVENT_TYPE = "NOTIFICATION";
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
