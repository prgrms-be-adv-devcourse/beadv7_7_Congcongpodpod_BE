package kr.lastdish.core.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventMessageTest {

  @Test
  void 직렬화된_이벤트와_발행_metadata를_제공한다() {
    // given
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();

    String payload =
        """
        {
          "schemaVersion": 1,
          "dishId": 10,
          "available": true,
          "stockQuantity": 5
        }
        """;

    // when
    EventMessage message =
        new EventMessage(eventId, "DISH_STATE_CHANGED", "DISH", 10L, payload, occurredAt);

    // then
    assertThat(message.eventId()).isEqualTo(eventId);
    assertThat(message.eventType()).isEqualTo("DISH_STATE_CHANGED");
    assertThat(message.aggregateType()).isEqualTo("DISH");
    assertThat(message.aggregateId()).isEqualTo(10L);
    assertThat(message.payload()).isEqualTo(payload);
    assertThat(message.occurredAt()).isEqualTo(occurredAt);
  }
}
