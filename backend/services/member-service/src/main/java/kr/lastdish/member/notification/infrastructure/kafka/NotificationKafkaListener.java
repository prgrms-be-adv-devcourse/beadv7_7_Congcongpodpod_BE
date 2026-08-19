package kr.lastdish.member.notification.infrastructure.kafka;

import kr.lastdish.common.event.EventHandlerRegistry;
import kr.lastdish.common.event.EventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationKafkaListener {

  public static final String CONSUMER_ID = "member-notification";
  public static final String TOPIC = "NOTIFICATION";

  private final EventHandlerRegistry handlerRegistry;

  @KafkaListener(topics = TOPIC, groupId = CONSUMER_ID)
  public void consume(EventMessage message) {
    handlerRegistry.get(CONSUMER_ID, message.eventType()).handle(message);
  }
}