package kr.lastdish.common.inbox.infrastructure;

import jakarta.persistence.LockModeType;
import kr.lastdish.common.inbox.domain.InboxAggregateVersion;
import kr.lastdish.common.inbox.domain.InboxAggregateVersionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InboxAggregateVersionJpaRepository
    extends JpaRepository<InboxAggregateVersion, InboxAggregateVersionId> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT version
      FROM InboxAggregateVersion version
      WHERE version.id = :id
      """)
  Optional<InboxAggregateVersion> findByIdForUpdate(@Param("id") InboxAggregateVersionId id);
}
