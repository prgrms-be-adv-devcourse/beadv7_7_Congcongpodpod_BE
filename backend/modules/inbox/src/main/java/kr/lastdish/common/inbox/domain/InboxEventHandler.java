package kr.lastdish.common.inbox.domain;

import kr.lastdish.common.event.EventHandler;
import kr.lastdish.common.event.EventMessage;

public interface InboxEventHandler extends EventHandler {

  default InboxProcessingPolicy processingPolicy() {
    return InboxProcessingPolicy.IDEMPOTENT;
  }

  default void onExhausted(EventMessage message, Throwable lastError) {}
}
