package kr.lastdish.common.event.kafka;

import java.util.concurrent.TimeUnit;
import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;

@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

  private static final long PUBLISH_TIMEOUT_SECONDS = 10;
  private final KafkaTemplate<String, EventMessage> kafkaTemplate;

  @Override
  public void publish(EventMessage message) {
    String topic = message.eventType();
    String key = message.aggregateType() + ":" + message.aggregateId();
    validateTopic(topic);

    try {
      kafkaTemplate.send(topic, key, message).get(PUBLISH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Kafka 이벤트 발행 중 인터럽트가 발생했습니다.", exception);
    } catch (Exception exception) {
      throw new IllegalStateException(
          "Kafka 이벤트 발행에 실패했습니다. eventId=" + message.eventId(), exception);
    }
  }

  private void validateTopic(String topic) {
    if (topic == null || !topic.matches("[a-zA-Z0-9._-]+")) {
      throw new IllegalArgumentException("Kafka 토픽으로 사용할 수 없는 eventType입니다: " + topic);
    }
  }
}
