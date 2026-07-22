package kr.lastdish.core.common.event.spring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.lastdish.core.common.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class SpringEventDeserializerTest {

  private static final String TEST_EVENT_TYPE = "TEST_EVENT";

  private SpringEventDeserializer deserializer;

  @BeforeEach
  void setUp() {
    SpringEventTypeResolver resolver =
        new SpringEventTypeResolver() {

          @Override
          public String eventType() {
            return TEST_EVENT_TYPE;
          }

          @Override
          public Class<? extends DomainEvent> eventClass() {
            return TestDomainEvent.class;
          }
        };

    deserializer = new SpringEventDeserializer(new ObjectMapper(), List.of(resolver));
  }

  @Test
  void 등록된_이벤트_타입의_payload를_복원한다() {
    // given
    UUID eventId = UUID.randomUUID();
    Instant occurredAt = Instant.now();

    String payload =
        """
        {
          "eventId": "%s",
          "schemaVersion": 1,
          "aggregateId": 10,
          "value": "변경값",
          "occurredAt": "%s"
        }
        """
            .formatted(eventId, occurredAt);

    // when
    DomainEvent restored = deserializer.deserialize(TEST_EVENT_TYPE, payload);

    // then
    assertThat(restored).isInstanceOf(TestDomainEvent.class);

    TestDomainEvent event = (TestDomainEvent) restored;

    assertThat(event.eventId()).isEqualTo(eventId);
    assertThat(event.schemaVersion()).isEqualTo(1);
    assertThat(event.aggregateId()).isEqualTo(10L);
    assertThat(event.value()).isEqualTo("변경값");
    assertThat(event.occurredAt()).isEqualTo(occurredAt);
  }

  @Test
  void 등록되지_않은_이벤트_타입이면_예외가_발생한다() {
    assertThatThrownBy(() -> deserializer.deserialize("UNKNOWN_EVENT", "{}"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("지원하지 않는 Spring 이벤트 타입입니다: UNKNOWN_EVENT");
  }

  @Test
  void 올바르지_않은_JSON이면_역직렬화에_실패한다() {
    assertThatThrownBy(() -> deserializer.deserialize(TEST_EVENT_TYPE, "{ invalid json }"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Spring 이벤트 역직렬화에 실패했습니다.");
  }

  private record TestDomainEvent(
      UUID eventId, int schemaVersion, Long aggregateId, String value, Instant occurredAt)
      implements DomainEvent {

    @Override
    public String eventType() {
      return TEST_EVENT_TYPE;
    }

    @Override
    public String aggregateType() {
      return "TEST";
    }
  }
}
