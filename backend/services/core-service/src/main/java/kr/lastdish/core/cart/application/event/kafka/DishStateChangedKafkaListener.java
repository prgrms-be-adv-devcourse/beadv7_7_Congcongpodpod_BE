package kr.lastdish.core.cart.application.event.kafka;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.application.InboxEventWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DishStateChangedKafkaListener {

  public static final String CONSUMER_ID = "core-cart-dish-state";

  private final InboxEventWriter inboxEventWriter;

  @KafkaListener(topics = "DISH_STATE_CHANGED", groupId = CONSUMER_ID)
  public void consume(EventMessage message) {
    inboxEventWriter.saveIfAbsent(CONSUMER_ID, message);
  }
}
