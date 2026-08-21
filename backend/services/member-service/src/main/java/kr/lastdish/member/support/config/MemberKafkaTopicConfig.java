package kr.lastdish.member.support.config;

import kr.lastdish.common.event.EventTopicResolver;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class MemberKafkaTopicConfig {

  public static final String MEMBER_EVENTS_TOPIC = "MEMBER_EVENTS";

  @Bean
  NewTopic notificationTopic() {
    return TopicBuilder.name("NOTIFICATION").partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic memberEventsTopic() {
    return TopicBuilder.name(MEMBER_EVENTS_TOPIC).partitions(3).replicas(1).build();
  }

  @Bean
  EventTopicResolver memberEventTopicResolver() {
    return message ->
        "MEMBER".equals(message.aggregateType()) ? MEMBER_EVENTS_TOPIC : message.eventType();
  }
}
