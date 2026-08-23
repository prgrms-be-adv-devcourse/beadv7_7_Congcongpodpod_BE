package kr.lastdish.core.settlement.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import kr.lastdish.core.settlement.application.SettlementEventAccumulator;
import kr.lastdish.core.settlement.application.event.kafka.OrderPickedUpKafkaListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OrderPickedUpMessageHandler implements InboxEventHandler {
    private final ObjectMapper objectMapper;
    private final SettlementEventAccumulator settlementEventAccumulator;

    @Override
    public String consumerId() {
        return OrderPickedUpKafkaListener.CONSUMER_ID;
    }

    @Override
    public String eventType() {
        return "ORDER_PICKED_UP";
    }

    @Override
    public void handle(EventMessage message) {
        OrderPickedUpPayload payload;
        try {
            payload = objectMapper.readValue(message.payload(), OrderPickedUpPayload.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("역직렬화 실패", e);
        }

        settlementEventAccumulator.accumulate(payload.orderId(), payload.storeId(), payload.finalOrderAmount(), payload.pickupResultAt());
    }
}
