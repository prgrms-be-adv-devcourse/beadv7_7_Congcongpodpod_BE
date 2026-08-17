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

  /** v2에서 payload에 정가(dishPrice)가 추가됐습니다. v1 payload에는 판매가만 있습니다. */
  public static final int SCHEMA_VERSION = 2;

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
