package kr.lastdish.payment.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record ChargeRequestedEvent(
        UUID eventId,
        int schemaVersion,
        Long paymentId,
        long aggregateVersion,
        ChargeRequestedPayload payload,
        Instant occurredAt)
        implements DomainEvent<ChargeRequestedPayload> {

    public static final String EVENT_TYPE = "CHARGE_REQUESTED";
    public static final int SCHEMA_VERSION = 1;

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public String aggregateType() {
        return "PAYMENT";
    }

    @Override
    public Long aggregateId() {
        return paymentId;
    }
}
