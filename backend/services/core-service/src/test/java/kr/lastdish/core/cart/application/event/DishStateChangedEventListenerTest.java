package kr.lastdish.core.cart.application.event;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishStateChangedEventListenerTest {

  @Mock private DishStateChangedMessageHandler handler;

  private DishStateChangedEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new DishStateChangedEventListener(handler);
  }

  @Test
  void Dish_상태_변경_메시지를_Handler에_전달한다() {
    // given
    EventMessage message = createMessage(DishStateChangedEventListener.EVENT_TYPE);

    // when
    listener.handle(message);

    // then
    verify(handler).handle(message);
  }

  @Test
  void 다른_타입의_메시지는_무시한다() {
    EventMessage message = createMessage("ORDER_COMPLETED");

    listener.handle(message);

    verify(handler, never()).handle(message);
  }

  private EventMessage createMessage(String eventType) {
    return new EventMessage(
        UUID.randomUUID(),
        eventType,
        "DISH",
        10L,
        1L,
        2,
        "{\"available\":true,\"stockQuantity\":5}",
        Instant.now());
  }
}
