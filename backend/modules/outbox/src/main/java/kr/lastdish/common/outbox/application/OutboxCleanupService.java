package kr.lastdish.common.outbox.application;

import kr.lastdish.common.outbox.domain.OutboxCleanupRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 다음 정리 주기까지 보관된 PUBLISHED 이벤트를 짧은 트랜잭션에서 삭제합니다. */
@Service
public class OutboxCleanupService {

  private final OutboxCleanupRepository repository;
  private final int batchSize;

  public OutboxCleanupService(
      OutboxCleanupRepository repository,
      @Value("${outbox.cleanup.batch-size:1000}") int batchSize) {
    this.repository = repository;
    this.batchSize = batchSize;
  }

  @Transactional
  public int cleanup() {
    return repository.deletePublishedBatch(batchSize);
  }
}
