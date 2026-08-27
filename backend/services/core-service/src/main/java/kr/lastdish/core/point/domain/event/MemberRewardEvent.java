package kr.lastdish.core.point.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

public record MemberRewardEvent(
    UUID eventId,
    int schemaVersion,
    Long memberId,
    long aggregateVersion,
    MemberRewardPayload payload,
    Instant occurredAt)
    implements DomainEvent<MemberRewardPayload> {

  public static final String EVENT_TYPE = "NOTIFICATION";
  public static final int SCHEMA_VERSION = 1;

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public String aggregateType() {
    return "NOTIFICATION";
  }

  @Override
  public Long aggregateId() {
    return memberId;
  }
}
