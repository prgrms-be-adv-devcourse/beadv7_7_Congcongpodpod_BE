package kr.lastdish.common.inbox.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import kr.lastdish.common.inbox.domain.InboxCleanupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** PostgreSQL의 SKIP LOCKED를 사용해 여러 인스턴스가 서로 다른 완료 이벤트를 정리하게 합니다. */
@Repository
@RequiredArgsConstructor
public class InboxCleanupRepositoryAdapter implements InboxCleanupRepository {

  private final EntityManager entityManager;

  @Override
  public int deleteCompletedBatch(Instant processedBefore, int batchSize) {
    String sql =
        """
        WITH candidates AS (
            SELECT consumer_id, event_id
              FROM inbox_events
             WHERE status IN ('PROCESSED', 'SKIPPED')
               AND processed_at < :processedBefore
             ORDER BY processed_at
             LIMIT :batchSize
             FOR UPDATE SKIP LOCKED
        )
        DELETE FROM inbox_events AS inbox
        USING candidates
        WHERE inbox.consumer_id = candidates.consumer_id
          AND inbox.event_id = candidates.event_id
        """;

    return entityManager
        .createNativeQuery(sql)
        .setParameter("processedBefore", processedBefore)
        .setParameter("batchSize", batchSize)
        .executeUpdate();
  }
}
