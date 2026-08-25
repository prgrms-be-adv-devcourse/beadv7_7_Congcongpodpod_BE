package kr.lastdish.core.support.config;

import kr.lastdish.core.dish.domain.event.DishPriceChangedEvent;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import kr.lastdish.core.order.domain.event.OrderPickedUpEvent;
import kr.lastdish.core.order.domain.event.OrderStatusChangedEvent;
import kr.lastdish.core.store.domain.event.StoreRegisteredEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class CoreKafkaTopicConfig {

  @Bean
  NewTopic orderStatusChangedTopic() {
    return TopicBuilder.name(OrderStatusChangedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }

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

  @Bean
  NewTopic storeCreatedTopic() {
    return TopicBuilder.name(kr.lastdish.core.store.domain.event.StoreCreatedEvent.EVENT_TYPE)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  NewTopic dishCreatedTopic() {
    return TopicBuilder.name(kr.lastdish.core.dish.domain.event.DishCreatedEvent.EVENT_TYPE)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  NewTopic storeChangedTopic() {
    return TopicBuilder.name(kr.lastdish.core.store.domain.event.StoreChangedEvent.EVENT_TYPE)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  NewTopic storeDeletedTopic() {
    return TopicBuilder.name(kr.lastdish.core.store.domain.event.StoreDeletedEvent.EVENT_TYPE)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  NewTopic storeStatusChangedTopic() {
    return TopicBuilder.name(kr.lastdish.core.store.domain.event.StoreStatusChangedEvent.EVENT_TYPE)
        .partitions(3)
        .replicas(1)
        .build();
  }

  @Bean
  NewTopic storeRegisteredTopic() {
    return TopicBuilder.name(StoreRegisteredEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic orderPickedUpTopic() {
    return TopicBuilder.name(OrderPickedUpEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }
}
