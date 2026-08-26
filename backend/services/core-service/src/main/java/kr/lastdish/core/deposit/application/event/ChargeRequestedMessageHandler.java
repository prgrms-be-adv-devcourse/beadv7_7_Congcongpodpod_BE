package kr.lastdish.core.deposit.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import kr.lastdish.common.inbox.domain.InboxExhaustionHandler;
import kr.lastdish.common.inbox.domain.InboxProcessingPolicy;
import kr.lastdish.core.deposit.application.DepositFacade;
import kr.lastdish.core.deposit.domain.DepositChargeFailure;
import kr.lastdish.core.deposit.domain.DepositChargeFailureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChargeRequestedMessageHandler implements InboxEventHandler, InboxExhaustionHandler {

  private final ObjectMapper objectMapper;
  private final DepositFacade depositFacade;
  private final DepositChargeFailureRepository depositChargeFailureRepository;

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

  @Override
  public void onExhausted(EventMessage message, Throwable lastError) {
    ChargeRequestedPayload payload;

    try {
      payload = objectMapper.readValue(message.payload(), ChargeRequestedPayload.class);
    } catch (JacksonException exception) {
      // payload 파싱조차 안 되는 경우 원본 정보를 로그로만 남김
      log.error(
          "예치금 충전 실패 기록 생성 중 payload 역직렬화 실패. eventId={}, payload={}",
          message.eventId(),
          message.payload(),
          exception);
      return;
    }

    depositChargeFailureRepository.save(
        DepositChargeFailure.record(
            payload.memberId(), message.aggregateId(), payload.amount(), lastError.getMessage()));
  }
}
