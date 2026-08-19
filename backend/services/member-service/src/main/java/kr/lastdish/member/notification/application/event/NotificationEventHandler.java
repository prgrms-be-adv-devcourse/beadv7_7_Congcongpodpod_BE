package kr.lastdish.member.notification.application.event;

import kr.lastdish.common.event.EventHandler;
import kr.lastdish.common.event.EventMessage;
import kr.lastdish.member.notification.application.NotificationService;
import kr.lastdish.member.notification.application.dto.CreateNotificationCommand;
import kr.lastdish.member.notification.infrastructure.kafka.NotificationKafkaListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class NotificationEventHandler implements EventHandler {

  public static final String EVENT_TYPE = "NOTIFICATION";

  private final ObjectMapper objectMapper;
  private final NotificationService notificationService;

  @Override
  public String consumerId() {
    return NotificationKafkaListener.CONSUMER_ID;
  }

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public void handle(EventMessage message) {
    NotificationEventPayload payload;
    try {
      payload = objectMapper.readValue(message.payload(), NotificationEventPayload.class);
    } catch (Exception exception) {
      throw new IllegalStateException("알림 이벤트 payload 역직렬화 실패", exception);
    }
    notificationService.createNotification(new CreateNotificationCommand(
        payload.memberId(),
        payload.type(),
        payload.title(),
        payload.body(),
        payload.data(),
        payload.linkTarget(),
        payload.linkId(),
        message.eventId()));
  }
}