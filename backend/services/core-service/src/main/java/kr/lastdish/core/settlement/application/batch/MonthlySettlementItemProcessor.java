package kr.lastdish.core.settlement.application.batch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.util.Map;
import java.util.stream.Collectors;
import kr.lastdish.core.settlement.application.SettlementEventService;
import kr.lastdish.core.settlement.application.dto.SettlementProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.spi.SessionImplementor;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlySettlementItemProcessor
    implements ItemProcessor<Long, SettlementProcessResult> {
  private final SettlementEventService settlementEventService;

  @Override
  public SettlementProcessResult process(Long settlementId) {
    log.info("=========================process settlementId :" + settlementId);
    logMemory(settlementId);
    logPersistenceContext(settlementId);
    return settlementEventService.processMonthlySettlement(settlementId);
  }

  private void logMemory(Long storeId) {
    Runtime runtime = Runtime.getRuntime();

    long used = runtime.totalMemory() - runtime.freeMemory();

    log.info(
        "정산 메모리. settlementId={}, used={}MB, committed={}MB, max={}MB",
        storeId,
        used / 1024 / 1024,
        runtime.totalMemory() / 1024 / 1024,
        runtime.maxMemory() / 1024 / 1024);
  }

  @PersistenceUnit private EntityManagerFactory entityManagerFactory;

  private void logPersistenceContext(Long storeId) {
    EntityManager currentEntityManager =
        EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);

    if (currentEntityManager == null) {
      log.info("현재 JPA 트랜잭션이 없습니다. settlementId={}", storeId);
      return;
    }

    SessionImplementor session = currentEntityManager.unwrap(SessionImplementor.class);

    int managedEntityCount = session.getPersistenceContext().getNumberOfManagedEntities();

    log.info("영속성 컨텍스트 상태. settlementId={}, managedEntities={}", storeId, managedEntityCount);

    Map<String, Long> entityCounts =
        session.getPersistenceContext().getEntitiesByKey().values().stream()
            .collect(
                Collectors.groupingBy(
                    entity -> entity.getClass().getSimpleName(), Collectors.counting()));

    log.info("영속성 컨텍스트 엔티티. settlementId={}, entities={}", storeId, entityCounts);
  }
}
