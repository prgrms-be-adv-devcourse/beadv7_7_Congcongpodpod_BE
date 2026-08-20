package kr.lastdish.member.support.config;

import kr.lastdish.member.notification.infrastructure.sse.NotificationRedisSubscriber;
import kr.lastdish.member.notification.infrastructure.sse.RedisSseEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class NotificationRedisConfig {

  @Bean
  public RedisTemplate<String, String> notificationRedisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, String> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new StringRedisSerializer());
    return template;
  }

  @Bean
  public RedisMessageListenerContainer notificationRedisContainer(
      RedisConnectionFactory factory, NotificationRedisSubscriber subscriber) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(factory);
    container.addMessageListener(subscriber, new ChannelTopic(RedisSseEventPublisher.CHANNEL));
    return container;
  }
}
