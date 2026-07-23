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

    DishStateChangedPayload payload = new DishStateChangedPayload(true, 5L);

    // when
    DishStateChangedEvent event =
        new DishStateChangedEvent(
            eventId, DishStateChangedEvent.SCHEMA_VERSION, 10L, 1L, payload, occurredAt);

    // then
    assertThat(event.eventId()).isEqualTo(eventId);
    assertThat(event.schemaVersion()).isEqualTo(DishStateChangedEvent.SCHEMA_VERSION);
    assertThat(event.dishId()).isEqualTo(10L);
    assertThat(event.aggregateVersion()).isEqualTo(1L);
    assertThat(event.occurredAt()).isEqualTo(occurredAt);

    assertThat(event.payload()).isEqualTo(payload);
    assertThat(event.payload().available()).isTrue();
    assertThat(event.payload().stockQuantity()).isEqualTo(5L);

    assertThat(event.eventType()).isEqualTo(DishStateChangedEvent.EVENT_TYPE);
    assertThat(event.aggregateType()).isEqualTo("DISH");
    assertThat(event.aggregateId()).isEqualTo(10L);
  }
}
