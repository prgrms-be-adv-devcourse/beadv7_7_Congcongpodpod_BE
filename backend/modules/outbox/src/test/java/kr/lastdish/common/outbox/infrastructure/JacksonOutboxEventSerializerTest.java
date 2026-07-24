package kr.lastdish.common.outbox.infrastructure;

import kr.lastdish.common.outbox.support.DishStateChangedEvent;
import kr.lastdish.common.outbox.support.DishStateChangedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonOutboxEventSerializerTest {

  private JacksonOutboxEventSerializer serializer;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    serializer = new JacksonOutboxEventSerializer(objectMapper);
  }

  @Test
  void Dish_이벤트를_Outbox_payload로_직렬화한다() {
    DishStateChangedPayload eventPayload =
        new DishStateChangedPayload(false, 5L);

    DishStateChangedEvent source =
        new DishStateChangedEvent(
            UUID.randomUUID(),
            DishStateChangedEvent.SCHEMA_VERSION,
            1L,
            3L,
            eventPayload,
            Instant.now());

    String payload = serializer.serialize(source.payload());

    assertThat(payload)
        .contains("\"available\":false")
        .contains("\"stockQuantity\":5")
        .doesNotContain("eventId")
        .doesNotContain("schemaVersion")
        .doesNotContain("dishId")
        .doesNotContain("aggregateVersion")
        .doesNotContain("occurredAt");
  }
}