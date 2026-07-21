package kr.lastdish.core.common.event.spring;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kr.lastdish.core.common.event.DomainEvent;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 직렬화된 EventMessage payload를 Spring Application Event 객체로 복원합니다.
 *
 * <p>각 도메인이 등록한 SpringEventTypeResolver를 사용하므로 구체적인 도메인 이벤트 클래스를 직접 참조하지 않습니다.
 */
@Component
public class SpringEventDeserializer {

  private final ObjectMapper objectMapper;
  private final Map<String, Class<? extends DomainEvent>> eventTypes;

  public SpringEventDeserializer(
      ObjectMapper objectMapper, List<SpringEventTypeResolver> resolvers) {

    this.objectMapper = objectMapper;
    this.eventTypes = createEventTypeRegistry(resolvers);
  }

  public DomainEvent deserialize(String eventType, String payload) {

    Class<? extends DomainEvent> eventClass = eventTypes.get(eventType);

    if (eventClass == null) {
      throw new IllegalArgumentException("지원하지 않는 Spring 이벤트 타입입니다: " + eventType);
    }

    try {
      return objectMapper.readValue(payload, eventClass);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Spring 이벤트 역직렬화에 실패했습니다.", exception);
    }
  }

  private Map<String, Class<? extends DomainEvent>> createEventTypeRegistry(
      List<SpringEventTypeResolver> resolvers) {

    Map<String, Class<? extends DomainEvent>> registry = new HashMap<>();

    for (SpringEventTypeResolver resolver : resolvers) {
      Class<? extends DomainEvent> previous =
          registry.putIfAbsent(resolver.eventType(), resolver.eventClass());

      if (previous != null) {
        throw new IllegalStateException("중복된 Spring 이벤트 타입입니다: " + resolver.eventType());
      }
    }

    return Map.copyOf(registry);
  }
}
