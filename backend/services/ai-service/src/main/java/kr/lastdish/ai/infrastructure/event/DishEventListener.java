package kr.lastdish.ai.infrastructure.event;

import kr.lastdish.common.event.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DishEventListener {

  private final DishEventHandler dishEventHandler;

  @KafkaListener(
      topics = "${spring.kafka.consumer.topic.dish-events:dish-events}",
      groupId = "${spring.kafka.consumer.group-id:ai-service-dish-group}")
  public void listen(EventMessage message) {
    log.info(
        "Kafka 이벤트 수신. eventType={}, aggregateId={}", message.eventType(), message.aggregateId());

    // DISH_CREATED 이벤트 타입인 경우 핸들러 호출
    if (dishEventHandler.eventType().equals(message.eventType())) {
      dishEventHandler.handle(message);
    }
  }
}
