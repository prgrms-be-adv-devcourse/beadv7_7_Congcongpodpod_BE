package kr.lastdish.common.event.config;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.event.EventPublisher;
import kr.lastdish.common.event.EventTopicResolver;
import kr.lastdish.common.event.publisher.kafka.KafkaEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;

@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(name = "event.publisher", havingValue = "kafka", matchIfMissing = true)
public class KafkaEventAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(EventTopicResolver.class)
  EventTopicResolver eventTopicResolver() {
    return EventMessage::eventType;
  }

  @Bean
  @ConditionalOnMissingBean(EventPublisher.class)
  EventPublisher kafkaEventPublisher(
      KafkaTemplate<String, EventMessage> kafkaTemplate, EventTopicResolver topicResolver) {
    return new KafkaEventPublisher(kafkaTemplate, topicResolver);
  }
}
