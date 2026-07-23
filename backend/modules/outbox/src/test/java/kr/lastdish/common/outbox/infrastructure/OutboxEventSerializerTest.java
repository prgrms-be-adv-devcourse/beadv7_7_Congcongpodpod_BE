package kr.lastdish.common.outbox.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.outbox.support.DishStateChangedEvent;
import kr.lastdish.common.outbox.support.DishStateChangedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class OutboxEventSerializerTest {

  private OutboxEventSerializer serializer;

  @BeforeEach
  void setUp() {
    /*
     * 현재 프로젝트는 Spring Boot 4와 Jackson 3을 사용합니다.
     * 따라서 tools.jackson.databind.ObjectMapper를 사용합니다.
     */
    ObjectMapper objectMapper = new ObjectMapper();

    serializer = new OutboxEventSerializer(objectMapper);
  }

  @Test
  void Dish_이벤트를_Outbox_payload로_직렬화한다() {
    // given
    DishStateChangedPayload eventPayload = new DishStateChangedPayload(false, 5L);

    DishStateChangedEvent source =
        new DishStateChangedEvent(
            UUID.randomUUID(),
            DishStateChangedEvent.SCHEMA_VERSION,
            1L,
            3L,
            eventPayload,
            Instant.now());

    // when
    String payload = serializer.serialize(source.payload());

    // then
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
