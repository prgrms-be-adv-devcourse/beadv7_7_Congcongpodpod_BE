package kr.lastdish.core.common.event.dish;

import kr.lastdish.core.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * Dish의 판매 가능 여부가 변경되었음을 알리는 이벤트입니다.
 *
 * <p>Cart 등 다른 도메인은 이 이벤트를 받아 해당 Dish와 관련된 파생 데이터를 최종 일관성 방식으로 갱신할 수 있습니다.
 *
 * <p>Dish 엔티티 전체를 전달하지 않고 Consumer에게 필요한 최소 정보만 전달합니다.
 */
public record DishAvailabilityChangedEvent(
    UUID eventId, int schemaVersion, Long dishId, boolean available, Instant occurredAt)
    implements DomainEvent {

  /** 저장된 Outbox payload의 역직렬화와 Kafka Topic 결정 등에 사용할 이벤트 타입입니다. */
  public static final String EVENT_TYPE = "DISH_AVAILABILITY_CHANGED";

  /** 현재 이벤트 계약의 스키마 버전입니다. */
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
