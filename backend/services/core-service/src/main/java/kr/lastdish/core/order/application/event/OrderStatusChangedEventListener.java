package kr.lastdish.core.order.application.event;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.core.order.domain.event.OrderStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderStatusChangedEventListener {

  private final OrderStatusChangedMessageHandler handler;

  @EventListener
  public void handle(EventMessage message) {
    if (!OrderStatusChangedEvent.EVENT_TYPE.equals(message.eventType())) {
      return;
    }
    handler.handle(message);
  }
}
