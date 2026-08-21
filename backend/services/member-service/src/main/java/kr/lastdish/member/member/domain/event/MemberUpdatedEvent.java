package kr.lastdish.member.member.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record MemberUpdatedEvent(
    UUID eventId,
    int schemaVersion,
    Long memberId,
    long aggregateVersion,
    MemberEventPayload payload,
    Instant occurredAt)
    implements DomainEvent<MemberEventPayload> {

  public static final String EVENT_TYPE = "MEMBER_UPDATED";
  public static final int SCHEMA_VERSION = 1;

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public String aggregateType() {
    return "MEMBER";
  }

  @Override
  public Long aggregateId() {
    return memberId;
  }
}
