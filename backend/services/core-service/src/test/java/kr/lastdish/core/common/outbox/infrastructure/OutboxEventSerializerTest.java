package kr.lastdish.core.common.outbox.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
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
    DishStateChangedEvent source =
        new DishStateChangedEvent(
            UUID.randomUUID(), DishStateChangedEvent.SCHEMA_VERSION, 1L, false, 5L, Instant.now());

    // when
    String payload = serializer.serialize(source);

    // then
    assertThat(payload)
        .contains("\"schemaVersion\":1")
        .contains("\"dishId\":1")
        .contains("\"available\":false")
        .contains("\"stockQuantity\":5");
  }
}
