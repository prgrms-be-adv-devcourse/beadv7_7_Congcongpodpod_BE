package kr.lastdish.core.dish.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record DishCreatedEvent(
    UUID eventId,
    int schemaVersion,
    Long dishId,
    long aggregateVersion,
    DishCreatedPayload payload,
    Instant occurredAt)
    implements DomainEvent<DishCreatedPayload> {

  public static final String EVENT_TYPE = "DISH_IS_CREATED";
  public static final int SCHEMA_VERSION = 1;

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public String aggregateType() {
    return "DISH";
  }

  @Override
  public Long aggregateId() {
    return dishId;
  }
}
