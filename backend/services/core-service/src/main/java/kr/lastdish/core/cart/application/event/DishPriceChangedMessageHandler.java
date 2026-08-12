package kr.lastdish.core.cart.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import kr.lastdish.common.inbox.domain.InboxProcessingPolicy;
import kr.lastdish.core.cart.application.CartDishPriceSynchronizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class DishPriceChangedMessageHandler implements InboxEventHandler {

  private final ObjectMapper objectMapper;
  private final CartDishPriceSynchronizer synchronizer;

  @Override
  public String consumerId() {
    return "core-cart-dish-price";
  }

  @Override
  public String eventType() {
    return "DISH_PRICE_CHANGED";
  }

  @Override
  public InboxProcessingPolicy processingPolicy() {
    return InboxProcessingPolicy.IDEMPOTENT_LATEST_WINS;
  }

  @Override
  public void handle(EventMessage message) {
    DishPriceChangedPayload payload;

    try {
      payload = objectMapper.readValue(message.payload(), DishPriceChangedPayload.class);
    } catch (JacksonException exception) {
      throw new IllegalStateException("Dish 가격 변경 이벤트 역직렬화에 실패했습니다.", exception);
    }

    synchronizer.synchronize(
        message.aggregateId(), payload.unitPrice(), message.aggregateVersion());
  }
}
