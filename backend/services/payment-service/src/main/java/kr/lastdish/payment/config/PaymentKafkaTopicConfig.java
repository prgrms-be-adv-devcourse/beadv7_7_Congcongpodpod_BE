package kr.lastdish.payment.config;

import kr.lastdish.payment.domain.event.ChargeRequestedEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class PaymentKafkaTopicConfig {

    @Bean
    NewTopic chargeRequestedTopic() {
        return TopicBuilder.name(ChargeRequestedEvent.EVENT_TYPE).partitions(3).replicas(1).build();
    }
}