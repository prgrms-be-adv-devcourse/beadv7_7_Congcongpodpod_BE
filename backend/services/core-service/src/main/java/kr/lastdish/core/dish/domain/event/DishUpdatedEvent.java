package kr.lastdish.core.dish.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record DishUpdatedEvent(
    UUID eventId,
    int schemaVersion,
    Long dishId,
    long aggregateVersion,
    DishAIEventPayload payload,
    Instant occurredAt)
    implements DomainEvent<DishAIEventPayload> {

  public static final String EVENT_TYPE = "DISH_IS_UPDATED";
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
