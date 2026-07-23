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
          "available": false,
          "stockQuantity": 5
        }
        """;

    // when
    EventMessage message =
        new EventMessage(eventId, "DISH_STATE_CHANGED", "DISH", 10L, 3L, 2, payload, occurredAt);

    // then
    assertThat(message.eventId()).isEqualTo(eventId);
    assertThat(message.eventType()).isEqualTo("DISH_STATE_CHANGED");
    assertThat(message.aggregateType()).isEqualTo("DISH");
    assertThat(message.aggregateId()).isEqualTo(10L);
    assertThat(message.aggregateVersion()).isEqualTo(3L);
    assertThat(message.schemaVersion()).isEqualTo(2);
    assertThat(message.payload()).isEqualTo(payload);
    assertThat(message.occurredAt()).isEqualTo(occurredAt);
  }
}
