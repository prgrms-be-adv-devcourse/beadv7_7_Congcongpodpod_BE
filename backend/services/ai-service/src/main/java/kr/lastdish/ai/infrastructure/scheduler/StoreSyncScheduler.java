package kr.lastdish.ai.infrastructure.scheduler;

import kr.lastdish.ai.application.StoreIndexerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "store.sync.scheduler.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class StoreSyncScheduler {

  private final StoreIndexerService storeIndexerService;

  @Scheduled(fixedRate = 60000)
  public void pollAndSyncStores() {
    try {
      // 최근 1분 내 변경된 데이터 동기화
      storeIndexerService.syncUpdatedStores(1);
    } catch (Exception e) {
      log.error("Store Polling 스케줄러 실행 중 예외 발생", e);
    }
  }
}
