package kr.lastdish.core.store.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record StoreCreatedEvent(
    UUID eventId,
    int schemaVersion,
    Long storeId,
    long aggregateVersion,
    StoreCreatedPayload payload,
    Instant occurredAt)
    implements DomainEvent<StoreCreatedPayload> {

  public static final String EVENT_TYPE = "STORE_CREATED";
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
