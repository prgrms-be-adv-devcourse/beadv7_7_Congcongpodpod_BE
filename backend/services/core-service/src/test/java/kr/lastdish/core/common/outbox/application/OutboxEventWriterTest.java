package kr.lastdish.core.common.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.common.outbox.domain.OutboxEvent;
import kr.lastdish.core.common.outbox.domain.OutboxEventRepository;
import kr.lastdish.core.common.outbox.domain.OutboxStatus;
import kr.lastdish.core.common.outbox.infrastructure.OutboxEventSerializer;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxEventWriterTest {

  @Mock private OutboxEventRepository repository;

  @Mock private OutboxEventSerializer serializer;

  private OutboxEventWriter writer;

  @BeforeEach
  void setUp() {
    writer = new OutboxEventWriter(repository, serializer);
  }

  @Test
  void 도메인_이벤트를_직렬화하여_Outbox에_저장한다() {
    // given
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();

    DishStateChangedEvent event =
        new DishStateChangedEvent(
            eventId, DishStateChangedEvent.SCHEMA_VERSION, 1L, 3L, false, 5L, occurredAt);

    String payload =
        """
        {
          "eventId": "%s",
          "dishId": 1,
          "available": false
        }
        """
            .formatted(eventId);

    when(serializer.serialize(event)).thenReturn(payload);

    /*
     * repository.save()에 전달된 실제 OutboxEvent를 검증하기 위해
     * ArgumentCaptor를 사용합니다.
     */
    ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);

    // when
    writer.append(event);

    // then
    verify(serializer).serialize(event);
    verify(repository).save(outboxCaptor.capture());

    OutboxEvent savedOutbox = outboxCaptor.getValue();

    assertThat(savedOutbox.getEventId()).isEqualTo(eventId);
    assertThat(savedOutbox.getEventType()).isEqualTo(DishStateChangedEvent.EVENT_TYPE);
    assertThat(savedOutbox.getAggregateType()).isEqualTo("DISH");
    assertThat(savedOutbox.getAggregateId()).isEqualTo(1L);
    assertThat(savedOutbox.getAggregateVersion()).isEqualTo(3L);
    assertThat(savedOutbox.getPayload()).isEqualTo(payload);
    assertThat(savedOutbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(savedOutbox.getRetryCount()).isZero();
    assertThat(savedOutbox.getOccurredAt()).isEqualTo(occurredAt);
  }
}
