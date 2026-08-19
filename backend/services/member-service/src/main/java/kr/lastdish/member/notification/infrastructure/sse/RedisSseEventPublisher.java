package kr.lastdish.member.notification.infrastructure.sse;

import kr.lastdish.member.notification.application.sse.NotificationSseEvent;
import kr.lastdish.member.notification.application.sse.SseEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class RedisSseEventPublisher implements SseEventPublisher {

  public static final String CHANNEL = "notifications";

  private final RedisTemplate<String, String> notificationRedisTemplate;
  private final ObjectMapper objectMapper;

  @Override
  public void publish(NotificationSseEvent event) {
    try {
      notificationRedisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(event));
    } catch (Exception exception) {
      throw new IllegalStateException("알림 SSE 메시지 직렬화 실패", exception);
    }
  }
}
