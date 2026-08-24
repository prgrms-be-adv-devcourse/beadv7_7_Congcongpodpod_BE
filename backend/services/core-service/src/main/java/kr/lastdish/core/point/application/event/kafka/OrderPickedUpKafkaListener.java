package kr.lastdish.core.point.application.event.kafka;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.application.InboxEventWriter;
import kr.lastdish.core.order.domain.event.OrderPickedUpEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPickedUpKafkaListener {

  public static final String CONSUMER_ID = "core-point-order-picked-up";

  private final InboxEventWriter inboxEventWriter;

  @KafkaListener(
      topics = OrderPickedUpEvent.EVENT_TYPE,
      groupId = CONSUMER_ID,
      autoStartup = "${event.kafka.listener-auto-startup:true}")
  public void consume(EventMessage message) {
    inboxEventWriter.saveIfAbsent(CONSUMER_ID, message);
  }
}
