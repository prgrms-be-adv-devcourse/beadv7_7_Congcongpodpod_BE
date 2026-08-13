package kr.lastdish.common.inbox.application;

import java.time.Instant;
import java.util.List;
import kr.lastdish.common.inbox.domain.InboxClaimRepository;
import kr.lastdish.common.inbox.domain.InboxEventId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxClaimService {

  private final InboxClaimRepository repository;
  private final int batchSize;
  private final long lockTimeoutSeconds;

  public InboxClaimService(
      InboxClaimRepository repository,
      @Value("${inbox.batch-size:100}") int batchSize,
      @Value("${inbox.lock-timeout-seconds:60}") long lockTimeoutSeconds) {
    this.repository = repository;
    this.batchSize = batchSize;
    this.lockTimeoutSeconds = lockTimeoutSeconds;
  }

  @Transactional
  public List<InboxEventId> claim() {
    Instant now = Instant.now();
    return repository.claim(batchSize, now, now.minusSeconds(lockTimeoutSeconds));
  }
}
