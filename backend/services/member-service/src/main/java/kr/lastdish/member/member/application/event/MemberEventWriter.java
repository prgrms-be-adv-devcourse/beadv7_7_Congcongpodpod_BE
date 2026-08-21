package kr.lastdish.member.member.application.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.member.member.domain.Member;
import kr.lastdish.member.member.domain.event.MemberCreatedEvent;
import kr.lastdish.member.member.domain.event.MemberDeletedEvent;
import kr.lastdish.member.member.domain.event.MemberEventPayload;
import kr.lastdish.member.member.domain.event.MemberUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberEventWriter {

  private final OutboxEventWriter outboxEventWriter;

  public void appendCreated(Member member) {
    outboxEventWriter.append(
        new MemberCreatedEvent(
            UUID.randomUUID(),
            MemberCreatedEvent.SCHEMA_VERSION,
            member.getId(),
            member.nextAggregateVersion(),
            payload(member),
            Instant.now()));
  }

  public void appendUpdated(Member member) {
    outboxEventWriter.append(
        new MemberUpdatedEvent(
            UUID.randomUUID(),
            MemberUpdatedEvent.SCHEMA_VERSION,
            member.getId(),
            member.nextAggregateVersion(),
            payload(member),
            Instant.now()));
  }

  public void appendDeleted(Member member) {
    outboxEventWriter.append(
        new MemberDeletedEvent(
            UUID.randomUUID(),
            MemberDeletedEvent.SCHEMA_VERSION,
            member.getId(),
            member.nextAggregateVersion(),
            payload(member),
            Instant.now()));
  }

  private MemberEventPayload payload(Member member) {
    return new MemberEventPayload(member.getName(), member.getPhone());
  }
}
