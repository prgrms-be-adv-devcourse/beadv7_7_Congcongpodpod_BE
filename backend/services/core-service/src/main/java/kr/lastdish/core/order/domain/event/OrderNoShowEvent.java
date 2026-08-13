package kr.lastdish.core.order.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

/** 주문이 노쇼 처리됐을 때 발행하는 이벤트입니다. */
public record OrderNoShowEvent(
    UUID eventId,
    int schemaVersion,
    Long orderId,
    long aggregateVersion,
    OrderNoShowPayload payload,
    Instant occurredAt)
    implements DomainEvent<OrderNoShowPayload> {

  public static final String EVENT_TYPE = "ORDER_NO_SHOW";
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
