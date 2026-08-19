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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final SseNotifier sseNotifier;

  @Transactional
  public NotificationResponse createNotification(CreateNotificationCommand command) {
    if (command.eventId() != null && notificationRepository.existsByEventId(command.eventId())) {
      return null; // 중복 이벤트 무시 — 멱등
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
    sseNotifier.notify(saved);
    return NotificationResponse.from(saved);
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
