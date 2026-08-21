package kr.lastdish.payment.support.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class PaymentKafkaTopicConfig {

  @Bean
  NewTopic notificationTopic() {
    return TopicBuilder.name("NOTIFICATION").partitions(3).replicas(1).build();
  }
}
