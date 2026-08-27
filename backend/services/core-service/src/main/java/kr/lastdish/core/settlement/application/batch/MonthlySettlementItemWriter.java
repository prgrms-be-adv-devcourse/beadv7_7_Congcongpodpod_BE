package kr.lastdish.core.settlement.application.batch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.util.Map;
import java.util.stream.Collectors;
import kr.lastdish.core.settlement.application.dto.SettlementProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionImplementor;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class MonthlySettlementItemWriter
    implements ItemWriter<SettlementProcessResult>, StepExecutionListener {
  private int targetCount;
  private int createdCount;
  private int retriedCount;
  private int skippedCount;
  private int failedCount;

  @Override
  public void write(Chunk<? extends SettlementProcessResult> items) throws Exception {
    for (SettlementProcessResult result : items) {
      targetCount++;

      switch (result.status()) {
        case CREATED -> createdCount++;
        case RETRIED -> retriedCount++;
        case SKIPPED -> skippedCount++;
        case FAILED -> failedCount++;
      }

      log.info(
          "매장별 월 정산 처리 결과. storeId={}, settlementId={}, status={}, message={}",
          result.storeId(),
          result.settlementId(),
          result.status(),
          result.message());
      logPersistenceContext(result.storeId());
    }
  }

  @PersistenceUnit private EntityManagerFactory entityManagerFactory;

  private void logPersistenceContext(Long storeId) {
    EntityManager currentEntityManager =
        EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);

    if (currentEntityManager == null) {
      log.info("현재 JPA 트랜잭션이 없습니다. storeId={}", storeId);
      return;
    }

    SessionImplementor session = currentEntityManager.unwrap(SessionImplementor.class);

    int managedEntityCount = session.getPersistenceContext().getNumberOfManagedEntities();

    log.info("영속성 컨텍스트 상태. storeId={}, managedEntities={}", storeId, managedEntityCount);

    Map<String, Long> entityCounts =
        session.getPersistenceContext().getEntitiesByKey().values().stream()
            .collect(
                Collectors.groupingBy(
                    entity -> entity.getClass().getSimpleName(), Collectors.counting()));

    log.info("영속성 컨텍스트 엔티티. storeId={}, entities={}", storeId, entityCounts);
  }

  @Override
  public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
    var context = stepExecution.getExecutionContext();
    context.putInt("targetStoreCount", targetCount);
    context.putInt("createdStoreCount", createdCount);
    context.putInt("retriedStoreCount", retriedCount);
    context.putInt("skippedStoreCount", skippedCount);
    context.putInt("failedStoreCount", failedCount);

    log.info(
        "월 정산 배치 완료. settlementMonth={}, targetCount={}, createdCount={}, retriedCount={}, skippedCount={}, failedCount={}",
        stepExecution.getJobParameters().getString("settlementMonth"),
        targetCount,
        createdCount,
        retriedCount,
        skippedCount,
        failedCount);

    return stepExecution.getExitStatus();
  }
}
