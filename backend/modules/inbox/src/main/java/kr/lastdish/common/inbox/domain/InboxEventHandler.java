package kr.lastdish.common.inbox.domain;

import kr.lastdish.common.event.EventMessage;

public interface InboxEventHandler {

  String consumerId();

  String eventType();

  default InboxProcessingPolicy processingPolicy() {
    return InboxProcessingPolicy.IDEMPOTENT;
  }

  void handle(EventMessage message);
}
