package kr.lastdish.member.notification.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import kr.lastdish.member.notification.domain.Notification;
import kr.lastdish.member.notification.domain.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

  private final JpaNotificationRepository jpaNotificationRepository;

  @Override
  public Notification save(Notification notification) {
    return jpaNotificationRepository.save(notification);
  }

  @Override
  public Page<Notification> findByMemberId(Long memberId, Pageable pageable) {
    return jpaNotificationRepository.findAllByMemberId(memberId, pageable);
  }

  @Override
  public Optional<Notification> findByIdAndMemberId(Long id, Long memberId) {
    return jpaNotificationRepository.findByIdAndMemberId(id, memberId);
  }

  @Override
  public boolean existsByEventId(UUID eventId) {
    return jpaNotificationRepository.existsByEventId(eventId);
  }

  @Override
  public long countUnreadByMemberId(Long memberId) {
    return jpaNotificationRepository.countByMemberIdAndReadYnFalse(memberId);
  }

  @Override
  public int markAllAsReadByMemberId(Long memberId) {
    return jpaNotificationRepository.markAllAsReadByMemberId(memberId);
  }
}
