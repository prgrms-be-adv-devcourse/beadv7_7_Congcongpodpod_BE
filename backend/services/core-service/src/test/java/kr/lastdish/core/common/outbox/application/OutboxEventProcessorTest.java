package kr.lastdish.core.common.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import kr.lastdish.core.common.event.EventPublisher;
import kr.lastdish.core.common.outbox.domain.OutboxEvent;
import kr.lastdish.core.common.outbox.domain.OutboxEventRepository;
import kr.lastdish.core.common.outbox.domain.OutboxStatus;
import kr.lastdish.core.common.outbox.infrastructure.OutboxEventSerializer;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxEventProcessorTest {

  @Mock private OutboxEventRepository repository;

  @Mock private OutboxEventSerializer serializer;

  @Mock private EventPublisher eventPublisher;

  private OutboxEventProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new OutboxEventProcessor(repository, serializer, eventPublisher);
  }

  @Test
  void publishes_processing_event_and_marks_it_as_published() {
    // given
    DishStateChangedEvent event = createDomainEvent();
    String payload = "{\"dishId\":1}";

    OutboxEvent outbox = OutboxEvent.create(event, payload);
    outbox.markProcessing(Instant.now());

    when(repository.findById(event.eventId())).thenReturn(Optional.of(outbox));

    when(serializer.deserialize(event.eventType(), payload)).thenReturn(event);

    // when
    processor.process(event.eventId());

    // then
    verify(eventPublisher).publish(event);

    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    assertThat(outbox.getPublishedAt()).isNotNull();
    assertThat(outbox.getLockedAt()).isNull();
  }

  @Test
  void does_not_mark_event_as_published_when_publish_fails() {
    // given
    DishStateChangedEvent event = createDomainEvent();

    OutboxEvent outbox = OutboxEvent.create(event, "{}");
    outbox.markProcessing(Instant.now());

    when(repository.findById(event.eventId())).thenReturn(Optional.of(outbox));

    when(serializer.deserialize(event.eventType(), outbox.getPayload())).thenReturn(event);

    RuntimeException publishException = new RuntimeException("이벤트 발행 실패");

    doThrow(publishException).when(eventPublisher).publish(event);

    // when & then
    assertThatThrownBy(() -> processor.process(event.eventId())).isSameAs(publishException);

    /*
     * 단위 테스트에서는 Spring 트랜잭션 Proxy가 적용되지 않지만,
     * markPublished()가 publish() 이후에 있으므로 실행되지 않았는지 확인합니다.
     */
    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
    assertThat(outbox.getPublishedAt()).isNull();
  }

  @Test
  void rejects_event_that_is_not_processing() {
    // given
    DishStateChangedEvent event = createDomainEvent();

    OutboxEvent outbox = OutboxEvent.create(event, "{}");

    when(repository.findById(event.eventId())).thenReturn(Optional.of(outbox));

    // when & then
    assertThatThrownBy(() -> processor.process(event.eventId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("PROCESSING 상태의 이벤트만 발행할 수 있습니다");

    verify(serializer, never()).deserialize(outbox.getEventType(), outbox.getPayload());

    verify(eventPublisher, never()).publish(org.mockito.ArgumentMatchers.any());
  }

  private DishStateChangedEvent createDomainEvent() {
    return new DishStateChangedEvent(
        UUID.randomUUID(), DishStateChangedEvent.SCHEMA_VERSION, 1L, false, 5L, Instant.now());
  }
}
