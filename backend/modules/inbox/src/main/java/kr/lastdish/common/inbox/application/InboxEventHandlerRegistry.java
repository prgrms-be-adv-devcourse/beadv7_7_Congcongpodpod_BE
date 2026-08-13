package kr.lastdish.common.inbox.application;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import org.springframework.stereotype.Component;

@Component
public class InboxEventHandlerRegistry {

  private final Map<String, InboxEventHandler> handlers;

  public InboxEventHandlerRegistry(List<InboxEventHandler> handlerList) {
    handlers =
        handlerList.stream()
            .collect(
                Collectors.toUnmodifiableMap(InboxEventHandler::consumerId, Function.identity()));
  }

  public InboxEventHandler get(String consumerId, String eventType) {
    InboxEventHandler handler =
        Optional.ofNullable(handlers.get(consumerId))
            .orElseThrow(() -> new IllegalStateException("Inbox Handler가 없습니다: " + consumerId));

    if (!handler.eventType().equals(eventType)) {
      throw new IllegalStateException(
          "Inbox eventType이 Handler와 다릅니다. consumerId=" + consumerId + ", eventType=" + eventType);
    }

    return handler;
  }
}
