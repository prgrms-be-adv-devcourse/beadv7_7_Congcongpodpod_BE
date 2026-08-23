package kr.lastdish.core.settlement.application.event.kafka;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.application.InboxEventWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderNoShowKafkaListener {
  public static final String CONSUMER_ID = "order-settlement-detail-noShow";

  private final InboxEventWriter inboxEventWriter;

  @KafkaListener(
      topics = "ORDER_NO_SHOW",
      groupId = CONSUMER_ID,
      autoStartup = "${event.kafka.listener-auto-startup:true}")
  public void consume(EventMessage message) {
    inboxEventWriter.saveIfAbsent(CONSUMER_ID, message);
  }
}
