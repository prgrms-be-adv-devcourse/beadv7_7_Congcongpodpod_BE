package kr.lastdish.core.common.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.common.event.dish.DishStateChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SpringEventPublisherTest {

  @Mock private ApplicationEventPublisher applicationEventPublisher;

  private SpringEventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    eventPublisher = new SpringEventPublisher(applicationEventPublisher);
  }

  @Test
  void publishes_domain_event_with_spring_event() {
    // given
    DomainEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(), DishStateChangedEvent.SCHEMA_VERSION, 1L, false, 0L, Instant.now());

    // when
    eventPublisher.publish(event);

    // then
    /*
     * SpringEventPublisher가 전달받은 이벤트를 변경하지 않고
     * ApplicationEventPublisher에 그대로 전달했는지 검증합니다.
     */
    verify(applicationEventPublisher).publishEvent(event);
  }

  @Test
  void propagates_exception_when_spring_event_fails() {
    // given
    DomainEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(), DishStateChangedEvent.SCHEMA_VERSION, 1L, false, 5L, Instant.now());

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
    assertThatThrownBy(() -> eventPublisher.publish(event)).isSameAs(publishException);
  }
}
