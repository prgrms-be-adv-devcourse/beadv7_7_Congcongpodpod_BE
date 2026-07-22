package kr.lastdish.core.cart.application.event;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.cart.application.CartDishStateSynchronizer;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DishStateChangedEventListenerTest {

  @Mock private CartDishStateSynchronizer synchronizer;

  private DishStateChangedEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new DishStateChangedEventListener(synchronizer);
  }

  @Test
  void Dish_상태_변경값을_Cart_동기화_서비스에_전달한다() {
    // given
    DishStateChangedEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(),
            DishStateChangedEvent.SCHEMA_VERSION,
            10L,
            1L,
            true,
            5L,
            Instant.now());

    // when
    listener.handle(event);

    // then
    verify(synchronizer).synchronize(10L, true, 5L, 1L);
  }
}
