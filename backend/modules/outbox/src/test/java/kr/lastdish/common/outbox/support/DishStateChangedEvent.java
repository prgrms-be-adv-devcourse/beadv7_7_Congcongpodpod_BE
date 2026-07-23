package kr.lastdish.common.outbox.support;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

/** Outbox 모듈 테스트에서 사용하는 서비스 비종속 도메인 이벤트 fixture입니다. */
public record DishStateChangedEvent(
    UUID eventId,
    int schemaVersion,
    Long dishId,
    long aggregateVersion,
    DishStateChangedPayload payload,
    Instant occurredAt)
    implements DomainEvent<DishStateChangedPayload> {

  public static final String EVENT_TYPE = "DISH_STATE_CHANGED";
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
