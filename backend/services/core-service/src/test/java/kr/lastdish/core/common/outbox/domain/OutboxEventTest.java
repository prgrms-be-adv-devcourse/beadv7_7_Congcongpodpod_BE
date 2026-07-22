package kr.lastdish.core.common.outbox.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import org.junit.jupiter.api.Test;

class OutboxEventTest {

  @Test
  void 도메인_이벤트로_PENDING_Outbox를_생성한다() {
    // given
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();

    DishStateChangedEvent event =
        new DishStateChangedEvent(
            eventId, DishStateChangedEvent.SCHEMA_VERSION, 1L, false, 5L, occurredAt);

    String payload =
        """
        {
          "eventId": "%s",
          "schemaVersion": 1,
          "dishId": 1,
          "available": false
        }
        """
            .formatted(eventId);

    // when
    OutboxEvent outbox = OutboxEvent.create(event, payload);

    // then
    assertThat(outbox.getEventId()).isEqualTo(eventId);
    assertThat(outbox.getEventType()).isEqualTo(DishStateChangedEvent.EVENT_TYPE);
    assertThat(outbox.getAggregateType()).isEqualTo("DISH");
    assertThat(outbox.getAggregateId()).isEqualTo(1L);
    assertThat(outbox.getPayload()).isEqualTo(payload);
    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(outbox.getRetryCount()).isZero();
    assertThat(outbox.getOccurredAt()).isEqualTo(occurredAt);
    assertThat(outbox.getLockedAt()).isNull();
    assertThat(outbox.getPublishedAt()).isNull();
  }

  @Test
  void Outbox를_PROCESSING으로_변경한다() {
    // given
    OutboxEvent outbox = createOutbox();
    Instant lockedAt = Instant.now();

    // when
    outbox.markProcessing(lockedAt);

    // then
    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
    assertThat(outbox.getLockedAt()).isEqualTo(lockedAt);
  }

  @Test
  void Outbox_발행_성공을_기록한다() {
    // given
    OutboxEvent outbox = createOutbox();
    outbox.markProcessing(Instant.now());

    Instant publishedAt = Instant.now();

    // when
    outbox.markPublished(publishedAt);

    // then
    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    assertThat(outbox.getPublishedAt()).isEqualTo(publishedAt);
    assertThat(outbox.getLockedAt()).isNull();
    assertThat(outbox.getLastError()).isNull();
  }

  @Test
  void 재시도_횟수가_남아있으면_PENDING으로_변경한다() {
    // given
    OutboxEvent outbox = createOutbox();
    outbox.markProcessing(Instant.now());

    // when
    outbox.recordFailure("일시적인 발행 오류", 5);

    // then
    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(outbox.getRetryCount()).isEqualTo(1);
    assertThat(outbox.getLastError()).isEqualTo("일시적인 발행 오류");
    assertThat(outbox.getLockedAt()).isNull();
  }

  @Test
  void 최대_재시도_횟수에_도달하면_FAILED로_변경한다() {
    // given
    OutboxEvent outbox = createOutbox();

    // when
    outbox.recordFailure("첫 번째 실패", 2);
    outbox.recordFailure("두 번째 실패", 2);

    // then
    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(outbox.getRetryCount()).isEqualTo(2);
    assertThat(outbox.getLastError()).isEqualTo("두 번째 실패");
  }

  /** 각 테스트에서 반복되는 기본 Outbox 생성을 담당합니다. */
  private OutboxEvent createOutbox() {
    DishStateChangedEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(), DishStateChangedEvent.SCHEMA_VERSION, 1L, false, 5L, Instant.now());

    return OutboxEvent.create(event, "{}");
  }
}
