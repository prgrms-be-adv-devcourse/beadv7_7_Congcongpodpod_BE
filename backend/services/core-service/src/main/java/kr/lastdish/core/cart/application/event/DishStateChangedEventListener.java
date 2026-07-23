package kr.lastdish.core.cart.application.event;

import kr.lastdish.common.event.EventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DishStateChangedEventListener {

  public static final String EVENT_TYPE = "DISH_STATE_CHANGED";

  private final DishStateChangedMessageHandler handler;

  @EventListener
  public void handle(EventMessage message) {
    if (!EVENT_TYPE.equals(message.eventType())) {
      return;
    }

    handler.handle(message);
  }
}
