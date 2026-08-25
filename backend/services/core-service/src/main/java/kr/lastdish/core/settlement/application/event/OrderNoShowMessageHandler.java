package kr.lastdish.core.settlement.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import kr.lastdish.core.settlement.application.SettlementEventAccumulator;
import kr.lastdish.core.settlement.application.event.kafka.OrderNoShowKafkaListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OrderNoShowMessageHandler implements InboxEventHandler {
  private final ObjectMapper objectMapper;
  private final SettlementEventAccumulator settlementEventAccumulator;

  @Override
  public String consumerId() {
    return OrderNoShowKafkaListener.CONSUMER_ID;
  }

  @Override
  public String eventType() {
    return "ORDER_NO_SHOW";
  }

  @Override
  public void handle(EventMessage message) {
    OrderNoShowPayload payload;
    try {
      payload = objectMapper.readValue(message.payload(), OrderNoShowPayload.class);
    } catch (JacksonException e) {
      throw new IllegalStateException("역직렬화 실패", e);
    }

    settlementEventAccumulator.accumulate(
        message.aggregateId(), payload.storeId(), payload.salesAmount(), payload.pickupResultAt());
  }
}
