package kr.lastdish.core.cart.application.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.cart.application.CartDishStateSynchronizer;
import kr.lastdish.core.common.event.EventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DishStateChangedMessageHandlerTest {

  @Mock private CartDishStateSynchronizer synchronizer;

  private DishStateChangedMessageHandler handler;

  @BeforeEach
  void setUp() {
    handler = new DishStateChangedMessageHandler(new ObjectMapper(), synchronizer);
  }

  @Test
  void Payload를_Cart_입력으로_변환한다() {
    EventMessage message = createMessage("{\"available\":true,\"stockQuantity\":5}");

    handler.handle(message);

    verify(synchronizer).synchronize(10L, true, 5L, 3L);
  }

  @Test
  void 잘못된_Payload이면_예외가_발생한다() {
    EventMessage message = createMessage("{ invalid json }");

    assertThatThrownBy(() -> handler.handle(message))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Dish 상태 변경 이벤트 역직렬화에 실패했습니다.");
  }

  private EventMessage createMessage(String payload) {
    return new EventMessage(
        UUID.randomUUID(),
        DishStateChangedEventListener.EVENT_TYPE,
        "DISH",
        10L,
        3L,
        2,
        payload,
        Instant.now());
  }
}
