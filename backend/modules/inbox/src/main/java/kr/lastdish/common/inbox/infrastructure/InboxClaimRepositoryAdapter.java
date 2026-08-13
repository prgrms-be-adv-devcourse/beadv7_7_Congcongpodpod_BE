package kr.lastdish.common.inbox.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.lastdish.common.inbox.domain.InboxClaimRepository;
import kr.lastdish.common.inbox.domain.InboxEventId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 처리할 Inbox 이벤트를 DB Lock으로 선점하는 Adapter입니다.
 *
 * <p>PostgreSQL의 {@code FOR UPDATE SKIP LOCKED}를 사용해 여러 서비스 인스턴스가 동시에 Scheduler를 실행하더라도 같은 이벤트를 중복
 * 선점하지 않게 합니다.
 *
 * <p>PROCESSING 상태에서 인스턴스가 종료될 수 있으므로 Lock이 만료된 이벤트도 다시 선점합니다.
 */
@Repository
@RequiredArgsConstructor
public class InboxClaimRepositoryAdapter implements InboxClaimRepository {

  private final EntityManager entityManager;

  /**
   * 처리 가능한 Inbox 이벤트를 일정 개수만큼 선점합니다.
   *
   * <p>조회와 PROCESSING 상태 변경을 하나의 SQL로 실행하여 원자성을 보장합니다.
   *
   * @param batchSize 한 번에 선점할 최대 이벤트 수
   * @param now 현재 선점 시각
   * @param lockExpiredBefore 이 시각보다 오래된 PROCESSING 이벤트를 만료로 판단
   * @return 선점된 복합 이벤트 식별자 목록
   */
  @Override
  public List<InboxEventId> claim(int batchSize, Instant now, Instant lockExpiredBefore) {
    String sql =
        """
        WITH candidates AS (
            SELECT consumer_id, event_id
            FROM inbox_events
            WHERE status = 'RECEIVED'
               OR (
                    status = 'PROCESSING'
                    AND locked_at < :lockExpiredBefore
               )
            ORDER BY received_at
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
        )
        UPDATE inbox_events AS inbox
        SET status = 'PROCESSING',
            locked_at = :now
        FROM candidates
        WHERE inbox.consumer_id = candidates.consumer_id
          AND inbox.event_id = candidates.event_id
        RETURNING inbox.consumer_id, inbox.event_id
        """;

    List<?> results =
        entityManager
            .createNativeQuery(sql)
            .setParameter("batchSize", batchSize)
            .setParameter("now", now)
            .setParameter("lockExpiredBefore", lockExpiredBefore)
            .getResultList();

    return results.stream().map(this::toInboxEventId).toList();
  }

  /** Native Query의 consumer_id와 event_id를 Inbox 복합 식별자로 변환합니다. */
  private InboxEventId toInboxEventId(Object value) {
    Object[] columns = (Object[]) value;

    String consumerId = columns[0].toString();
    UUID eventId = toUuid(columns[1]);

    return new InboxEventId(consumerId, eventId);
  }

  private UUID toUuid(Object value) {
    if (value instanceof UUID uuid) {
      return uuid;
    }

    return UUID.fromString(value.toString());
  }
}
