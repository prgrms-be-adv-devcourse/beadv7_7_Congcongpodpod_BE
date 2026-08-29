package kr.lastdish.common.outbox.infrastructure;

import jakarta.persistence.EntityManager;
import kr.lastdish.common.outbox.domain.OutboxCleanupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** PostgreSQL의 SKIP LOCKED를 사용해 여러 인스턴스가 서로 다른 완료 이벤트를 정리하게 합니다. */
@Repository
@RequiredArgsConstructor
public class OutboxCleanupRepositoryAdapter implements OutboxCleanupRepository {

  private final EntityManager entityManager;

  @Override
  public int deletePublishedBatch(int batchSize) {
    String sql =
        """
        WITH candidates AS (
            SELECT event_id
              FROM outbox_events
             WHERE status = 'PUBLISHED'
             ORDER BY published_at
             LIMIT :batchSize
             FOR UPDATE SKIP LOCKED
        )
        DELETE FROM outbox_events AS outbox
        USING candidates
        WHERE outbox.event_id = candidates.event_id
        """;

    return entityManager
        .createNativeQuery(sql)
        .setParameter("batchSize", batchSize)
        .executeUpdate();
  }
}
