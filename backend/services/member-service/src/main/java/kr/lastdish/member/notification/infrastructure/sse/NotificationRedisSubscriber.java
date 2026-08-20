package kr.lastdish.member.notification.infrastructure.sse;

import kr.lastdish.member.notification.application.sse.NotificationSseEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class NotificationRedisSubscriber implements MessageListener {

  private final SseEmitterRegistry emitterRegistry;
  private final ObjectMapper objectMapper;

  @Override
  public void onMessage(Message message, byte[] pattern) {
    try {
      NotificationSseEvent event =
          objectMapper.readValue(message.getBody(), NotificationSseEvent.class);
      emitterRegistry.send(event.memberId(), event.notification());
    } catch (Exception exception) {
      throw new IllegalStateException("알림 SSE 메시지 역직렬화 실패", exception);
    }
  }
}
