package kr.lastdish.core.cart.application.event;

import kr.lastdish.common.event.EventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DishPriceChangedEventListener {

  public static final String EVENT_TYPE = "DISH_PRICE_CHANGED";

  private final DishPriceChangedMessageHandler handler;

  @EventListener
  public void handle(EventMessage message) {
    if (!EVENT_TYPE.equals(message.eventType())) {
      return;
    }

    handler.handle(message);
  }
}
