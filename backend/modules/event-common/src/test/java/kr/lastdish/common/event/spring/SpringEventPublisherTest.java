package kr.lastdish.common.event.spring;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.EventMessage;
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
  void EventMessage를_Spring_Event로_그대로_발행한다() {
    EventMessage message =
        new EventMessage(
            UUID.randomUUID(),
            "DISH_STATE_CHANGED",
            "DISH",
            10L,
            1L,
            2,
            "{\"available\":true,\"stockQuantity\":5}",
            Instant.now());

    eventPublisher.publish(message);

    verify(applicationEventPublisher).publishEvent(message);
  }

  @Test
  void propagates_exception_when_spring_event_fails() {
    // given
    EventMessage message =
        new EventMessage(
            UUID.randomUUID(),
            "DISH_STATE_CHANGED",
            "DISH",
            1L,
            1L,
            2,
            "{\"available\":false,\"stockQuantity\":0}",
            Instant.now());

    RuntimeException publishException = new RuntimeException("Spring Event 발행 실패");

    /*
     * ApplicationEventPublisher가 이벤트를 발행할 때 예외를 발생시키도록 설정합니다.
     */
    doThrow(publishException).when(applicationEventPublisher).publishEvent(message);

    // when & then
    /*
     * SpringEventPublisher가 예외를 삼키지 않고 호출자에게 전달해야
     * Outbox Processor가 실패를 감지하고 재시도할 수 있습니다.
     */
    assertThatThrownBy(() -> eventPublisher.publish(message)).isSameAs(publishException);
  }
}
