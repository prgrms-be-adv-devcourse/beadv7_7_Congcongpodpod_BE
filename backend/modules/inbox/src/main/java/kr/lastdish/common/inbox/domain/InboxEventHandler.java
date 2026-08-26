package kr.lastdish.common.inbox.domain;

import kr.lastdish.common.event.EventHandler;

public interface InboxEventHandler extends EventHandler {

  default InboxProcessingPolicy processingPolicy() {
    return InboxProcessingPolicy.IDEMPOTENT;
  }
}
