package kr.lastdish.core.common.event.dish;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DishAvailabilityChangedEventTest {

  @Test
  void Dish_판매_가능_상태_변경_이벤트를_생성한다() {
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();

    DishAvailabilityChangedEvent event =
        new DishAvailabilityChangedEvent(
            eventId,
            DishAvailabilityChangedEvent.SCHEMA_VERSION,
            1L,
            false,
            occurredAt
        );

    assertThat(event.eventId()).isEqualTo(eventId);
    assertThat(event.schemaVersion()).isEqualTo(1);
    assertThat(event.eventType())
        .isEqualTo("DISH_AVAILABILITY_CHANGED");
    assertThat(event.aggregateType()).isEqualTo("DISH");
    assertThat(event.aggregateId()).isEqualTo(1L);
    assertThat(event.available()).isFalse();
    assertThat(event.occurredAt()).isEqualTo(occurredAt);
  }
}