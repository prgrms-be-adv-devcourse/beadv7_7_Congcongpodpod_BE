package kr.lastdish.common.api.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.json.JsonWriter;

class SystemZoneTimestampCustomizerTests {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final Instant SAMPLE = Instant.parse("2026-08-19T07:09:41.342172Z");

  /** ECS 형식이 @timestamp를 Instant로 담는 구조를 그대로 흉내 낸다. */
  private JsonWriter<Instant> writerWith(SystemZoneTimestampCustomizer<Instant> customizer) {
    return JsonWriter.of(
        members -> {
          members.add("@timestamp", instant -> instant);
          members.add("message", instant -> "테스트 메시지");
          customizer.customize(members);
        });
  }

  @Test
  @DisplayName("@timestamp를 시스템 시간대 오프셋 표기로 바꾼다")
  void timestamp를_서울_시간대로_바꾼다() {
    String json = writerWith(new SystemZoneTimestampCustomizer<>(SEOUL)).writeToString(SAMPLE);

    assertThat(json).contains("\"@timestamp\":\"2026-08-19T16:09:41.342172+09:00\"");
  }

  @Test
  @DisplayName("@timestamp 외의 값은 건드리지 않는다")
  void 다른_필드는_그대로_둔다() {
    String json = writerWith(new SystemZoneTimestampCustomizer<>(SEOUL)).writeToString(SAMPLE);

    assertThat(json).contains("\"message\":\"테스트 메시지\"");
  }
}
