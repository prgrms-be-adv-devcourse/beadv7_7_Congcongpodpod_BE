package kr.lastdish.core.store.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record StoreRegisteredEvent(
    UUID eventId,
    int schemaVersion,
    Long storeId,
    long aggregateVersion,
    StoreRegisteredPayload payload,
    Instant occurredAt)
    implements DomainEvent<StoreRegisteredPayload> {

  public static final String EVENT_TYPE = "STORE_REGISTERED";
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
