package kr.lastdish.core.cart.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import kr.lastdish.common.inbox.domain.InboxProcessingPolicy;
import kr.lastdish.core.cart.application.CartDishStateSynchronizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class DishStateChangedMessageHandler implements InboxEventHandler {

  private final ObjectMapper objectMapper;
  private final CartDishStateSynchronizer synchronizer;

  @Override
  public String consumerId() {
    return "core-cart-dish-state";
  }

  @Override
  public String eventType() {
    return "DISH_STATE_CHANGED";
  }

  @Override
  public InboxProcessingPolicy processingPolicy() {
    return InboxProcessingPolicy.IDEMPOTENT_LATEST_WINS;
  }

  @Override
  public void handle(EventMessage message) {
    DishStateChangedPayload payload;

    try {
      payload = objectMapper.readValue(message.payload(), DishStateChangedPayload.class);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Dish 상태 변경 이벤트 역직렬화에 실패했습니다.", exception);
    }

    synchronizer.synchronize(
        message.aggregateId(),
        payload.available(),
        payload.stockQuantity(),
        message.aggregateVersion());
  }
}
