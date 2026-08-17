package kr.lastdish.core.store.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record StoreChangedEvent(
    UUID eventId,
    int schemaVersion,
    Long storeId,
    long aggregateVersion,
    StoreChangedPayload payload,
    Instant occurredAt)
    implements DomainEvent<StoreChangedPayload> {

  public static final String EVENT_TYPE = "STORE_INFO_CHANGED";
  public static final int SCHEMA_VERSION = 1;

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public String aggregateType() {
    return "STORE";
  }

  @Override
  public Long aggregateId() {
    return storeId;
  }
}
