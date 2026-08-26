package kr.lastdish.ai.infrastructure.scheduler;

import java.time.Instant;
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
  private static final long OVERLAP_SECONDS = 10; // 시차 오차에 대비하여 겹치게 조회
  private final StoreSyncWatermarkStore watermarkStore;

  @Scheduled(fixedRate = 60000)
  public void pollAndSyncStores() {
    Instant from = null;
    Instant to = null;
    // getLastSyncedAt() 호출이 try 밖에 있다면 redis 장애 시 이 호출에서 던진 예외가 캐치 안 되고 스케줄러 메서드 자체가 예외로 죽을 수 있음
    try {
      Instant lastSyncedAt = watermarkStore.getLastSyncedAt();
      from = lastSyncedAt.minusSeconds(OVERLAP_SECONDS);
      to = Instant.now();

      // 최근 1분 + OVERLAP_SECONDS 내 변경된 데이터 동기화
      storeIndexerService.syncUpdatedStores(from, to);
      watermarkStore.updateLastSyncedAt(to); // 성공하면 전진
      log.info("Store Polling 동기화 완료. from={}, to={}", from, to);
    } catch (Exception e) {
      // watermark 갱신 X
      log.error("Store Polling 스케줄러 실행 중 예외 발생. from={}, to={}", from, to, e);
    }
  }

  @Scheduled(fixedRate = 60000)
  public void retryFailedEmbeddings() {
    storeIndexerService.retryFailedEmbeddings();
  }
}
