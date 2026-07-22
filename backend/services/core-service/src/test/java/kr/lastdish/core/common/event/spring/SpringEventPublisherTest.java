package kr.lastdish.core.common.event.spring;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.common.event.DomainEvent;
import kr.lastdish.core.common.event.EventMessage;
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

  @Mock private SpringEventDeserializer eventDeserializer;

  private SpringEventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    eventPublisher = new SpringEventPublisher(applicationEventPublisher, eventDeserializer);
  }

  @Test
  void EventMessage를_역직렬화하여_Spring_Event로_발행한다() {
    // given
    DomainEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(),
            DishStateChangedEvent.SCHEMA_VERSION,
            1L,
            1L,
            false,
            0L,
            Instant.now());
    EventMessage message = createMessage(event, "{\"dishId\":1}");

    when(eventDeserializer.deserialize(message.eventType(), message.payload())).thenReturn(event);

    // when
    eventPublisher.publish(message);

    // then
    verify(eventDeserializer).deserialize(message.eventType(), message.payload());
    verify(applicationEventPublisher).publishEvent(event);
  }

  @Test
  void propagates_exception_when_spring_event_fails() {
    // given
    DomainEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(),
            DishStateChangedEvent.SCHEMA_VERSION,
            1L,
            1L,
            false,
            5L,
            Instant.now());
    EventMessage message = createMessage(event, "{\"dishId\":1}");

    when(eventDeserializer.deserialize(message.eventType(), message.payload())).thenReturn(event);

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
        event.aggregateVersion(),
        payload,
        event.occurredAt());
  }
}
