package kr.lastdish.core.deposit.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import kr.lastdish.common.inbox.domain.InboxProcessingPolicy;
import kr.lastdish.core.deposit.application.DepositFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ChargeRequestedMessageHandler implements InboxEventHandler {

  private final ObjectMapper objectMapper;
  private final DepositFacade depositFacade;

  @Override
  public String consumerId() {
    return "core-deposit-charge-requested";
  }

  @Override
  public String eventType() {
    return "CHARGE_REQUESTED";
  }

  @Override
  public InboxProcessingPolicy processingPolicy() {
    return InboxProcessingPolicy.IDEMPOTENT;
  }

  @Override
  public void handle(EventMessage message) {
    ChargeRequestedPayload payload;

    try {
      payload = objectMapper.readValue(message.payload(), ChargeRequestedPayload.class);
    } catch (JacksonException exception) {
      throw new IllegalStateException(
          "예치금 충전 요청 이벤트 역직렬화에 실패했습니다. eventId="
              + message.eventId()
              + ", eventType="
              + message.eventType(),
          exception);
    }
    depositFacade.charge(payload.memberId(), message.aggregateId(), payload.amount());
  }
}
