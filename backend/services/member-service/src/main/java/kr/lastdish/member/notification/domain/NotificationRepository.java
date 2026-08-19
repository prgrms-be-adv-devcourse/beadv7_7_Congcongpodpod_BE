package kr.lastdish.member.notification.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository {

  Notification save(Notification notification);

  Page<Notification> findByMemberId(Long memberId, Pageable pageable);

  Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

  long countUnreadByMemberId(Long memberId);

  int markAllAsReadByMemberId(Long memberId);

  boolean existsByEventId(UUID eventId);
}
