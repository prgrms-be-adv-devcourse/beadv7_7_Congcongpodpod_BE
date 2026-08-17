package kr.lastdish.core.store.domain.event;

import kr.lastdish.common.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record StoreDeletedEvent(
        UUID eventId,
        int schemaVersion,
        Long storeId,
        long aggregateVersion,
        StoreDeletedPayload payload,
        Instant occurredAt)
        implements DomainEvent<StoreDeletedPayload> {

    public static final String EVENT_TYPE = "STORE_IS_DELETED";
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
