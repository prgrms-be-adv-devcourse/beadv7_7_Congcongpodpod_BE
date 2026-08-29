package kr.lastdish.common.inbox.application;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import kr.lastdish.common.inbox.domain.InboxCleanupRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kafka 재전달에 대한 멱등성 보관기간이 지난 Inbox 완료 이벤트를 정리합니다. */
@Service
public class InboxCleanupService {

  private final InboxCleanupRepository repository;
  private final int retentionDays;
  private final int batchSize;

  public InboxCleanupService(
      InboxCleanupRepository repository,
      @Value("${inbox.cleanup.retention-days:3}") int retentionDays,
      @Value("${inbox.cleanup.batch-size:1000}") int batchSize) {
    this.repository = repository;
    this.retentionDays = retentionDays;
    this.batchSize = batchSize;
  }

  @Transactional
  public int cleanup() {
    Instant processedBefore = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
    return repository.deleteCompletedBatch(processedBefore, batchSize);
  }
}
