package kr.lastdish.common.api.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * ECS 구조화 로그의 {@code @timestamp}를 시스템 시간대 기준으로 바꾼다.
 *
 * <p>Boot의 ECS 형식은 {@code @timestamp}를 {@link java.time.Instant}로 담아 UTC(`...Z`)로 직렬화한다. 운영 컨테이너는
 * {@code TZ=Asia/Seoul}로 뜨므로 기존 콘솔 로그는 {@code +09:00}이었는데, JSON 전환만 하면 9시간 어긋난 값으로 보이게 된다. 그 차이를
 * 없애기 위해 값을 가로채 오프셋 표기로 바꾼다.
 */
public class SystemZoneTimestampCustomizer<T> implements StructuredLoggingJsonMembersCustomizer<T> {

  private final ZoneId zone;

  /** Boot가 설정값으로 클래스 이름만 받아 생성하므로 기본 생성자가 반드시 있어야 한다. */
  public SystemZoneTimestampCustomizer() {
    this(ZoneId.systemDefault());
  }

  SystemZoneTimestampCustomizer(ZoneId zone) {
    this.zone = zone;
  }

  @Override
  public void customize(JsonWriter.Members<T> members) {
    members.applyingValueProcessor(
        JsonWriter.ValueProcessor.<Object>of(this::toZonedText).whenHasPath("@timestamp"));
  }

  /** Instant면 시스템 시간대 오프셋 표기 문자열로 바꾸고, 그 외 타입은 손대지 않는다. */
  private Object toZonedText(Object value) {
    if (value instanceof Instant instant) {
      return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atZone(zone));
    }
    return value;
  }
}
