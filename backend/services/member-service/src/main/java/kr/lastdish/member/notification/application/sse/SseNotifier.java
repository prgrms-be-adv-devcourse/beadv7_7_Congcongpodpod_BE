package kr.lastdish.member.notification.application.sse;

import kr.lastdish.member.notification.domain.Notification;
import kr.lastdish.member.notification.presentation.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseNotifier {

  private final SseEventPublisher sseEventPublisher;

  public void notify(Notification notification) {
    sseEventPublisher.publish(
        new NotificationSseEvent(
            notification.getMemberId(), NotificationResponse.from(notification)));
  }
}