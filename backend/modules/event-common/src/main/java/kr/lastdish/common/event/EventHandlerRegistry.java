package kr.lastdish.common.event;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class EventHandlerRegistry {

  private final Map<String, EventHandler> handlers;

  public EventHandlerRegistry(List<EventHandler> handlerList) {
    handlers =
        handlerList.stream()
            .collect(Collectors.toUnmodifiableMap(EventHandler::consumerId, Function.identity()));
  }

  public EventHandler get(String consumerId, String eventType) {
    EventHandler handler =
        Optional.ofNullable(handlers.get(consumerId))
            .orElseThrow(() -> new IllegalStateException("Event Handler가 없습니다: " + consumerId));

    if (!handler.eventType().equals(eventType)) {
      throw new IllegalStateException(
          "eventType이 Handler와 다릅니다. consumerId=" + consumerId + ", eventType=" + eventType);
    }

    return handler;
  }
}
