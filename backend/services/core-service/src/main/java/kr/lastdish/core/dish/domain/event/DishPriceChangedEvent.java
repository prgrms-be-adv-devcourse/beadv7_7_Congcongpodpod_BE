package kr.lastdish.core.dish.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record DishPriceChangedEvent(
    UUID eventId,
    int schemaVersion,
    Long dishId,
    long aggregateVersion,
    DishPriceChangedPayload payload,
    Instant occurredAt)
    implements DomainEvent<DishPriceChangedPayload> {

  public static final String EVENT_TYPE = "DISH_PRICE_CHANGED";
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
