package kr.lastdish.core.dish.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DishStateChangedEventTest {

  @Test
  void Dish_상태_변경에_필요한_정보를_제공한다() {
    // given
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();

    // when
    DishStateChangedEvent event =
        new DishStateChangedEvent(
            eventId, DishStateChangedEvent.SCHEMA_VERSION, 1L, true, 5L, occurredAt);

    // then
    assertThat(event.eventId()).isEqualTo(eventId);
    assertThat(event.schemaVersion()).isEqualTo(DishStateChangedEvent.SCHEMA_VERSION);
    assertThat(event.dishId()).isEqualTo(1L);
    assertThat(event.available()).isTrue();
    assertThat(event.stockQuantity()).isEqualTo(5L);
    assertThat(event.occurredAt()).isEqualTo(occurredAt);
    assertThat(event.eventType()).isEqualTo(DishStateChangedEvent.EVENT_TYPE);
    assertThat(event.aggregateType()).isEqualTo("DISH");
    assertThat(event.aggregateId()).isEqualTo(1L);
  }
}
