package kr.lastdish.core.order.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.core.order.application.MemberSnapshotSynchronizer;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class MemberUpdatedMessageHandler extends AbstractMemberMessageHandler {
  public static final String CONSUMER_ID = "core-order-member-updated";
  public static final String EVENT_TYPE = "MEMBER_UPDATED";

  public MemberUpdatedMessageHandler(
      ObjectMapper objectMapper, MemberSnapshotSynchronizer synchronizer) {
    super(objectMapper, synchronizer);
  }

  @Override
  public String consumerId() {
    return CONSUMER_ID;
  }

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public void handle(EventMessage message) {
    MemberEventPayload payload = payload(message);
    synchronizer.upsert(message.aggregateId(), payload.name(), payload.phone());
  }
}
