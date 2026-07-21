package kr.lastdish.core.common.event.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.common.event.DomainEvent;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SpringEventDeserializerTest {

  private SpringEventDeserializer deserializer;

  @BeforeEach
  void setUp() {
    deserializer = new SpringEventDeserializer(new ObjectMapper());
  }

  @Test
  void Dish_상태_변경_payload를_Spring_Event로_복원한다() {
    // given
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();

    String payload =
        """
        {
          "eventId": "%s",
          "schemaVersion": 1,
          "dishId": 10,
          "available": true,
          "stockQuantity": 5,
          "occurredAt": "%s"
        }
        """
            .formatted(eventId, occurredAt);

    // when
    DomainEvent restored = deserializer.deserialize(DishStateChangedEvent.EVENT_TYPE, payload);

    // then
    assertThat(restored).isInstanceOf(DishStateChangedEvent.class);

    DishStateChangedEvent event = (DishStateChangedEvent) restored;

    assertThat(event.eventId()).isEqualTo(eventId);
    assertThat(event.dishId()).isEqualTo(10L);
    assertThat(event.available()).isTrue();
    assertThat(event.stockQuantity()).isEqualTo(5L);
    assertThat(event.occurredAt()).isEqualTo(occurredAt);
  }

  @Test
  void 지원하지_않는_이벤트_타입이면_예외가_발생한다() {
    assertThatThrownBy(() -> deserializer.deserialize("UNKNOWN_EVENT", "{}"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("지원하지 않는 Spring 이벤트 타입입니다: UNKNOWN_EVENT");
  }

  @Test
  void 올바르지_않은_JSON이면_역직렬화에_실패한다() {
    assertThatThrownBy(
            () -> deserializer.deserialize(DishStateChangedEvent.EVENT_TYPE, "{ invalid json }"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Spring 이벤트 역직렬화에 실패했습니다.");
  }
}
