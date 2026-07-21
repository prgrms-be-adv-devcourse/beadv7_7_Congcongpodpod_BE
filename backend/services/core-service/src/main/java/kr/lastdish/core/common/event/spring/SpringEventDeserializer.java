package kr.lastdish.core.common.event.spring;

import kr.lastdish.core.common.event.DomainEvent;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 직렬화된 EventMessage payload를 Spring Application Event 객체로 복원합니다.
 *
 * <p>Spring Event는 Java 객체를 기반으로 Listener를 선택하므로 현재 활성화된 이벤트 타입과 Java 클래스를 이 어댑터에서 연결합니다. Kafka
 * Publisher는 payload를 그대로 전달하므로 이 역직렬화 과정에 의존하지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class SpringEventDeserializer {

  private final ObjectMapper objectMapper;

  public DomainEvent deserialize(String eventType, String payload) {

    try {
      if (DishStateChangedEvent.EVENT_TYPE.equals(eventType)) {
        return objectMapper.readValue(payload, DishStateChangedEvent.class);
      }

      throw new IllegalArgumentException("지원하지 않는 Spring 이벤트 타입입니다: " + eventType);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Spring 이벤트 역직렬화에 실패했습니다.", exception);
    }
  }
}
