package kr.lastdish.core.common.outbox.infrastructure;

import kr.lastdish.core.common.event.DomainEvent;
import kr.lastdish.core.common.event.dish.DishStateChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

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

  public DomainEvent deserialize(String eventType, String payload) {
    try {
      if (DishStateChangedEvent.EVENT_TYPE.equals(eventType)) {
        return objectMapper.readValue(payload, DishStateChangedEvent.class);
      }

      throw new IllegalArgumentException("지원하지 않는 이벤트 타입입니다: " + eventType);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Outbox 이벤트 역직렬화에 실패했습니다.", exception);
    }
  }
}
