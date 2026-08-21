package kr.lastdish.common.event;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class EventHandlerRegistry {

  private final Map<HandlerKey, EventHandler> handlers;

  public EventHandlerRegistry(List<EventHandler> handlerList) {
    handlers =
        handlerList.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    handler -> new HandlerKey(handler.consumerId(), handler.eventType()),
                    Function.identity()));
  }

  public EventHandler get(String consumerId, String eventType) {
    EventHandler handler =
        Optional.ofNullable(handlers.get(new HandlerKey(consumerId, eventType)))
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Event Handler가 없습니다: consumerId="
                            + consumerId
                            + ", eventType="
                            + eventType));

    return handler;
  }

  private record HandlerKey(String consumerId, String eventType) {}
}
