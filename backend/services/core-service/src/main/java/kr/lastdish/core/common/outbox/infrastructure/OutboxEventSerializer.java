package kr.lastdish.core.common.outbox.infrastructure;

import kr.lastdish.core.common.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * DomainEvent를 Outbox payload JSON으로 직렬화합니다.
 *
 * <p>이 클래스는 Outbox 저장만 담당하며 Spring Event 또는 Kafka 발행 기술을 알지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventSerializer {

  private final ObjectMapper objectMapper;

  public String serialize(DomainEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Outbox 이벤트 직렬화에 실패했습니다.", exception);
    }
  }
}
