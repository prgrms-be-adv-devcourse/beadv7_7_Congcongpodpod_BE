package kr.lastdish.core.support.config;

import kr.lastdish.core.dish.domain.event.DishPriceChangedEvent;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class CoreKafkaTopicConfig {

  @Bean
  NewTopic notificationTopic() {
    return TopicBuilder.name("NOTIFICATION").partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic dishStateChangedTopic() {
    return TopicBuilder.name(DishStateChangedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic dishPriceChangedTopic() {
    return TopicBuilder.name(DishPriceChangedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }

  /*
  @Bean
  NewTopic dishCreatedTopic() {
    return TopicBuilder.name(DishCreatedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic storeChangedTopic() {
    return TopicBuilder.name(StoreChangedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic storeDeletedTopic() {
    return TopicBuilder.name(StoreDeletedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic storeStatusChangedTopic() {
    return TopicBuilder.name(StoreStatusChangedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }
  */
}
