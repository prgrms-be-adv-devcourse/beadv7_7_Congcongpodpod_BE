package kr.lastdish.common.outbox.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxClaimRepository {

  /**
   * PENDING 또는 잠금이 만료된 PROCESSING 이벤트를 선점합니다.
   *
   * @param batchSize 한 번에 선점할 최대 이벤트 수
   * @param now 현재 선점 시각
   * @param lockExpiredBefore 잠금 만료 기준 시각
   * @return 선점한 이벤트 식별자
   */
  List<UUID> claim(int batchSize, Instant now, Instant lockExpiredBefore);
}
