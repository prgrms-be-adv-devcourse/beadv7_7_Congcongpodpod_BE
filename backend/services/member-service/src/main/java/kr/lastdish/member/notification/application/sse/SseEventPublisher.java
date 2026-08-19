package kr.lastdish.member.notification.application.sse;

public interface SseEventPublisher {
  void publish(NotificationSseEvent event);
}