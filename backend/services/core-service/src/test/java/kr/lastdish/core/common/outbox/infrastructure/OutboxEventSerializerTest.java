package kr.lastdish.core.common.outbox.infrastructure;

import kr.lastdish.core.common.event.DomainEvent;
import kr.lastdish.core.common.event.dish.DishAvailabilityChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
  void serializes_and_deserializes_dish_event() {
    // given
    DishAvailabilityChangedEvent source =
        new DishAvailabilityChangedEvent(
            UUID.randomUUID(),
            DishAvailabilityChangedEvent.SCHEMA_VERSION,
            1L,
            false,
            Instant.now()
        );

    // when
    String payload = serializer.serialize(source);

    DomainEvent restored =
        serializer.deserialize(
            source.eventType(),
            payload
        );

    // then
    assertThat(payload)
        .contains("\"dishId\":1")
        .contains("\"available\":false");

    assertThat(restored)
        .isInstanceOf(DishAvailabilityChangedEvent.class)
        .isEqualTo(source);
  }

  @Test
  void throws_when_event_type_is_unsupported() {
    // given
    String unsupportedEventType = "UNKNOWN_EVENT";

    // when & then
    assertThatThrownBy(
        () -> serializer.deserialize(
            unsupportedEventType,
            "{}"
        )
    )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(
            "지원하지 않는 이벤트 타입입니다: UNKNOWN_EVENT"
        );
  }

  @Test
  void throws_when_json_is_invalid() {
    // given
    String invalidPayload = "{ invalid json }";

    // when & then
    assertThatThrownBy(
        () -> serializer.deserialize(
            DishAvailabilityChangedEvent.EVENT_TYPE,
            invalidPayload
        )
    )
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "Outbox 이벤트 역직렬화에 실패했습니다."
        );
  }
}