package kr.lastdish.core.order.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import kr.lastdish.common.inbox.domain.InboxProcessingPolicy;
import kr.lastdish.core.order.application.MemberSnapshotSynchronizer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

abstract class AbstractMemberMessageHandler implements InboxEventHandler {
  protected final ObjectMapper objectMapper;
  protected final MemberSnapshotSynchronizer synchronizer;

  protected AbstractMemberMessageHandler(
      ObjectMapper objectMapper, MemberSnapshotSynchronizer synchronizer) {
    this.objectMapper = objectMapper;
    this.synchronizer = synchronizer;
  }

  @Override
  public InboxProcessingPolicy processingPolicy() {
    return InboxProcessingPolicy.IDEMPOTENT_LATEST_WINS;
  }

  protected MemberEventPayload payload(EventMessage message) {
    try {
      return objectMapper.readValue(message.payload(), MemberEventPayload.class);
    } catch (JacksonException exception) {
      throw new IllegalStateException("회원 이벤트 역직렬화에 실패했습니다.", exception);
    }
  }
}
