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
  아래 토픽은 리스너 구현 시 주석을 해제한다. 해제할 때 import 4줄을 함께 추가해야 컴파일된다.
  (spotless가 미사용 import를 지우므로 지금 미리 넣어둘 수 없다)

  import kr.lastdish.core.dish.domain.event.DishCreatedEvent;
  import kr.lastdish.core.store.domain.event.StoreChangedEvent;
  import kr.lastdish.core.store.domain.event.StoreDeletedEvent;
  import kr.lastdish.core.store.domain.event.StoreStatusChangedEvent;

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
