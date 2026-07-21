package kr.lastdish.core.common.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.common.outbox.infrastructure.OutboxEventSerializer;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpringEventPublisherTest {

  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @Mock private OutboxEventSerializer serializer;

  private SpringEventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    eventPublisher = new SpringEventPublisher(applicationEventPublisher, serializer);
  }

  @Test
  void EventMessage를_역직렬화하여_Spring_Event로_발행한다() {
    // given
    DomainEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(), DishStateChangedEvent.SCHEMA_VERSION, 1L, false, 0L, Instant.now());
    EventMessage message = createMessage(event, "{\"dishId\":1}");

    when(serializer.deserialize(message.eventType(), message.payload())).thenReturn(event);

    // when
    eventPublisher.publish(message);

    // then
    verify(serializer).deserialize(message.eventType(), message.payload());
    verify(applicationEventPublisher).publishEvent(event);
  }

  @Test
  void propagates_exception_when_spring_event_fails() {
    // given
    DomainEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(), DishStateChangedEvent.SCHEMA_VERSION, 1L, false, 5L, Instant.now());
    EventMessage message = createMessage(event, "{\"dishId\":1}");

    when(serializer.deserialize(message.eventType(), message.payload())).thenReturn(event);

    RuntimeException publishException = new RuntimeException("Spring Event 발행 실패");

    /*
     * ApplicationEventPublisher가 이벤트를 발행할 때 예외를 발생시키도록 설정합니다.
     */
    doThrow(publishException).when(applicationEventPublisher).publishEvent(event);

    // when & then
    /*
     * SpringEventPublisher가 예외를 삼키지 않고 호출자에게 전달해야
     * Outbox Processor가 실패를 감지하고 재시도할 수 있습니다.
     */
    assertThatThrownBy(() -> eventPublisher.publish(message)).isSameAs(publishException);
  }

  private EventMessage createMessage(DomainEvent event, String payload) {
    return new EventMessage(
        event.eventId(),
        event.eventType(),
        event.aggregateType(),
        event.aggregateId(),
        payload,
        event.occurredAt());
  }
}
