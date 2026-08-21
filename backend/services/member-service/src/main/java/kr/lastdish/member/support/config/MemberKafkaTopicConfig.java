package kr.lastdish.member.support.config;

import kr.lastdish.member.member.domain.event.MemberCreatedEvent;
import kr.lastdish.member.member.domain.event.MemberDeletedEvent;
import kr.lastdish.member.member.domain.event.MemberUpdatedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class MemberKafkaTopicConfig {

  @Bean
  NewTopic notificationTopic() {
    return TopicBuilder.name("NOTIFICATION").partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic memberCreatedTopic() {
    return TopicBuilder.name(MemberCreatedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic memberUpdatedTopic() {
    return TopicBuilder.name(MemberUpdatedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic memberDeletedTopic() {
    return TopicBuilder.name(MemberDeletedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
  }
}
