package kr.lastdish.common.outbox.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.lastdish.common.outbox.domain.OutboxClaimRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 발행할 Outbox 이벤트를 짧은 DB 트랜잭션에서 선점합니다.
 *
 * <p>이벤트 선점 트랜잭션과 실제 발행 트랜잭션을 분리합니다. 이벤트 발행 중에 DB Lock을 계속 유지하면 긴 트랜잭션과 Lock 경합이 발생할 수 있기 때문입니다.
 */
@Service
public class OutboxClaimService {

  private final OutboxClaimRepository claimRepository;
  private final int batchSize;
  private final long lockTimeoutSeconds;

  public OutboxClaimService(
      OutboxClaimRepository claimRepository,
      @Value("${outbox.batch-size:100}") int batchSize,
      @Value("${outbox.lock-timeout-seconds:60}") long lockTimeoutSeconds) {
    this.claimRepository = claimRepository;
    this.batchSize = batchSize;
    this.lockTimeoutSeconds = lockTimeoutSeconds;
  }

  /** PENDING 이벤트 또는 잠금이 만료된 PROCESSING 이벤트를 선점합니다. */
  @Transactional
  public List<UUID> claim() {
    Instant now = Instant.now();

    /*
     * PROCESSING 상태에서 서버가 종료된 이벤트는 lockedAt이 timeout보다
     * 오래되면 다른 Scheduler가 다시 선점할 수 있습니다.
     */
    Instant lockExpiredBefore = now.minus(Duration.ofSeconds(lockTimeoutSeconds));

    return claimRepository.claim(batchSize, now, lockExpiredBefore);
  }
}
