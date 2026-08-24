package kr.lastdish.member.member.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import kr.lastdish.member.member.application.MemberService;
import kr.lastdish.member.member.application.event.kafka.StoreRegisteredKafkaListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class StoreRegisteredMessageHandler implements InboxEventHandler {
  private final ObjectMapper objectMapper;
  private final MemberService memberService;

  @Override
  public String consumerId() {
    return StoreRegisteredKafkaListener.CONSUMER_ID;
  }

  @Override
  public String eventType() {
    return "STORE_REGISTERED";
  }

  @Override
  public void handle(EventMessage message) {
    StoreRegisteredPayload payload;
    try {
      payload = objectMapper.readValue(message.payload(), StoreRegisteredPayload.class);
    } catch (JacksonException e) {
      throw new IllegalStateException("역직렬화 실패", e);
    }
    memberService.grantSellerRole(payload.memberId());
  }
}
