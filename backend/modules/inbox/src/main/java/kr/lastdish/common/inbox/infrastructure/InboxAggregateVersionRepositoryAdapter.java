package kr.lastdish.common.inbox.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import kr.lastdish.common.inbox.domain.InboxAggregateVersion;
import kr.lastdish.common.inbox.domain.InboxAggregateVersionId;
import kr.lastdish.common.inbox.domain.InboxAggregateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InboxAggregateVersionRepositoryAdapter implements InboxAggregateVersionRepository {

  private final EntityManager entityManager;
  private final InboxAggregateVersionJpaRepository jpaRepository;

  @Override
  public InboxAggregateVersion getOrCreateAndLock(InboxAggregateVersionId id, Instant now) {
    /*
     * 두 Worker가 최초 이벤트를 동시에 처리해도 한 행만 생성되도록
     * PostgreSQL ON CONFLICT DO NOTHING을 사용한다.
     */
    entityManager
        .createNativeQuery(
            """
            INSERT INTO inbox_aggregate_versions (
                consumer_id,
                aggregate_type,
                aggregate_id,
                last_processed_version,
                updated_at
            )
            VALUES (
                :consumerId,
                :aggregateType,
                :aggregateId,
                0,
                :updatedAt
            )
            ON CONFLICT (
                consumer_id,
                aggregate_type,
                aggregate_id
            ) DO NOTHING
            """)
        .setParameter("consumerId", id.getConsumerId())
        .setParameter("aggregateType", id.getAggregateType())
        .setParameter("aggregateId", id.getAggregateId())
        .setParameter("updatedAt", now)
        .executeUpdate();

    return jpaRepository
        .findByIdForUpdate(id)
        .orElseThrow(() -> new IllegalStateException("Inbox Aggregate 버전 행을 찾을 수 없습니다: " + id));
  }
}
