package kr.lastdish.common.outbox.infrastructure;

import kr.lastdish.common.outbox.domain.OutboxEventSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * DomainEvent의 처리할 payload를 Outbox 저장용 JSON으로 직렬화합니다.
 *
 * <p>이벤트 메타데이터는 OutboxEvent의 개별 컬럼에 저장하며, 이 클래스는 처리할 payload만 JSON으로 변환합니다.
 */
@Component
@RequiredArgsConstructor
public class JacksonOutboxEventSerializer implements OutboxEventSerializer {

  private final ObjectMapper objectMapper;

  @Override
  public String serialize(Object payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Outbox 이벤트 payload 직렬화에 실패했습니다.", exception);
    }
  }
}
