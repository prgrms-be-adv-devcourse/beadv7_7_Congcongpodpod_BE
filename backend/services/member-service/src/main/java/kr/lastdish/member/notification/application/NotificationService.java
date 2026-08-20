package kr.lastdish.member.notification.application;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.notification.application.dto.CreateNotificationCommand;
import kr.lastdish.member.notification.application.sse.SseNotifier;
import kr.lastdish.member.notification.domain.Notification;
import kr.lastdish.member.notification.domain.NotificationRepository;
import kr.lastdish.member.notification.exception.NotificationErrorCode;
import kr.lastdish.member.notification.presentation.dto.NotificationResponse;
import kr.lastdish.member.notification.presentation.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final SseNotifier sseNotifier;

  @Transactional
  public NotificationResponse createNotification(CreateNotificationCommand command) {
    if (command.eventId() != null && notificationRepository.existsByEventId(command.eventId())) {
      return null;
    }

    Notification notification =
        new Notification(
            command.memberId(),
            command.type(),
            command.title(),
            command.body(),
            command.data(),
            command.linkTarget(),
            command.linkId(),
            command.eventId());

    Notification saved = notificationRepository.save(notification);

    notifySseQuietly(saved); // SSE 실패해도 롤백되지 않게

    return NotificationResponse.from(saved);
  }

  private void notifySseQuietly(Notification saved) {
    try {
      sseNotifier.notify(saved);
    } catch (Exception exception) {
      // 로그만 남기고 무시 — 알림은 이미 저장됨(DB가 원천)
      log.warn("SSE 알림 전송 실패 (알림은 저장됨): memberId={}", saved.getMemberId(), exception);
    }
  }

  public PageResponse<NotificationResponse> getNotifications(Long memberId, Pageable pageable) {
    return PageResponse.from(
        notificationRepository.findByMemberId(memberId, pageable).map(NotificationResponse::from));
  }

  @Transactional
  public void markAsRead(Long memberId, Long notificationId) {
    Notification notification =
        notificationRepository
            .findByIdAndMemberId(notificationId, memberId)
            .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
    notification.markAsRead();
  }

  @Transactional
  public void markAllAsRead(Long memberId) {
    notificationRepository.markAllAsReadByMemberId(memberId);
  }

  public long getUnreadCount(Long memberId) {
    return notificationRepository.countUnreadByMemberId(memberId);
  }
}
