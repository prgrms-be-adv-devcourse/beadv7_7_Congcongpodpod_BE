package kr.lastdish.core.dish.infrastructure.event;

import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DishStateChangedSpringEventTypeResolverTest {

  private final DishStateChangedSpringEventTypeResolver resolver =
      new DishStateChangedSpringEventTypeResolver();

  @Test
  void Dish_상태_변경_이벤트_타입과_클래스를_등록한다() {
    assertThat(resolver.eventType()).isEqualTo(DishStateChangedEvent.EVENT_TYPE);

    assertThat(resolver.eventClass()).isEqualTo(DishStateChangedEvent.class);
  }
}
