package kr.lastdish.member.notification.infrastructure.persistence;

import kr.lastdish.member.notification.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaNotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findAllByMemberId(Long memberId, Pageable pageable);

  Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

  long countByMemberIdAndReadYnFalse(Long memberId);

  boolean existsByEventId(UUID eventId);

  @Modifying
  @Query(
      "update Notification n set n.readYn = true "
          + "where n.memberId = :memberId and n.readYn = false")
  int markAllAsReadByMemberId(@Param("memberId") Long memberId);
}
