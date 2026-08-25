package kr.lastdish.core.point.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import kr.lastdish.common.inbox.domain.InboxProcessingPolicy;
import kr.lastdish.core.order.domain.event.OrderPickedUpEvent;
import kr.lastdish.core.order.domain.event.OrderPickedUpPayload;
import kr.lastdish.core.point.application.PointFacade;
import kr.lastdish.core.point.application.event.kafka.PointOrderPickedUpKafkaListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class PointOrderPickedUpMessageHandler implements InboxEventHandler {

  private final ObjectMapper objectMapper;
  private final PointFacade pointFacade;

  @Override
  public String consumerId() {
    return PointOrderPickedUpKafkaListener.CONSUMER_ID;
  }

  @Override
  public String eventType() {
    return OrderPickedUpEvent.EVENT_TYPE;
  }

  @Override
  public InboxProcessingPolicy processingPolicy() {
    return InboxProcessingPolicy.IDEMPOTENT;
  }

  @Override
  public void handle(EventMessage message) {
    OrderPickedUpPayload payload;
    try {
      payload = objectMapper.readValue(message.payload(), OrderPickedUpPayload.class);
    } catch (JacksonException exception) {
      throw new IllegalStateException("ORDER_PICKED_UP 이벤트 역직렬화에 실패했습니다.", exception);
    }

    Long orderId = message.aggregateId();

    pointFacade.earnAndEvaluateLevel(
        payload.memberId(), orderId, payload.finalOrderAmount(), payload.savedAmount());
  }
}
