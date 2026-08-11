package kr.lastdish.common.event.kafka.configureration;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.event.EventPublisher;
import kr.lastdish.common.event.kafka.KafkaEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(
    name = "event.publisher",
    havingValue = "kafka"
)
public class KafkaEventAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(EventPublisher.class)
  EventPublisher kafkaEventPublisher(
      KafkaTemplate<String, EventMessage> kafkaTemplate
  ) {
    return new KafkaEventPublisher(kafkaTemplate);
  }
}