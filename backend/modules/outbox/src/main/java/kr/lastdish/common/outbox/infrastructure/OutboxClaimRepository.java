package kr.lastdish.common.outbox.infrastructure;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 발행할 Outbox 이벤트를 DB Lock으로 선점합니다.
 *
 * <p>PostgreSQL의 FOR UPDATE SKIP LOCKED를 사용하여 여러 Core Service 인스턴스가 동시에 Scheduler를 실행하더라도 같은 이벤트를
 * 중복 선점하지 않게 합니다.
 *
 * <p>PROCESSING 상태에서 서버가 종료될 수 있으므로 lockedAt이 만료된 이벤트도 다시 선점 대상으로 포함합니다.
 */
@Repository
@RequiredArgsConstructor
public class OutboxClaimRepository {

  private final EntityManager entityManager;

  /**
   * 발행 가능한 Outbox 이벤트를 일정 개수만큼 선점합니다.
   *
   * <p>선점한 이벤트는 같은 쿼리 안에서 PROCESSING으로 변경합니다. 조회와 상태 변경을 분리하면 여러 인스턴스가 같은 PENDING 이벤트를 조회할 수 있으므로
   * 하나의 원자적인 SQL로 처리합니다.
   *
   * @param batchSize 한 번에 선점할 최대 이벤트 수
   * @param now 현재 선점 시각
   * @param lockExpiredBefore 이 시각보다 오래된 PROCESSING 이벤트를 만료로 판단
   * @return 선점된 이벤트 식별자 목록
   */
  public List<UUID> claim(int batchSize, Instant now, Instant lockExpiredBefore) {
    String sql =
        """
        WITH candidates AS (
            SELECT event_id
              FROM outbox_events
             WHERE (
                    status = 'PENDING'
                    OR (
                        status = 'PROCESSING'
                        AND locked_at < :lockExpiredBefore
                    )
                   )
             ORDER BY occurred_at
             LIMIT :batchSize
             FOR UPDATE SKIP LOCKED
        )
        UPDATE outbox_events AS outbox
           SET status = 'PROCESSING',
               locked_at = :now
          FROM candidates
         WHERE outbox.event_id = candidates.event_id
        RETURNING outbox.event_id
        """;

    /*
     * Native Query의 반환 타입은 JDBC Driver와 Hibernate 설정에 따라
     * UUID 또는 문자열처럼 전달될 수 있으므로 UUID로 안전하게 변환합니다.
     */
    List<?> results =
        entityManager
            .createNativeQuery(sql)
            .setParameter("batchSize", batchSize)
            .setParameter("now", now)
            .setParameter("lockExpiredBefore", lockExpiredBefore)
            .getResultList();

    return results.stream().map(this::toUuid).toList();
  }

  /** Native Query 결과를 UUID로 변환합니다. */
  private UUID toUuid(Object value) {
    if (value instanceof UUID uuid) {
      return uuid;
    }

    return UUID.fromString(value.toString());
  }
}
