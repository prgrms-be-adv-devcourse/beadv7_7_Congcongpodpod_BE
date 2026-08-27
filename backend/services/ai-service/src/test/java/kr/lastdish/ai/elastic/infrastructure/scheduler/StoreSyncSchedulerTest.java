package kr.lastdish.ai.elastic.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Optional;
import kr.lastdish.ai.elastic.application.StoreIndexerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreSyncSchedulerTest {

  @Mock private StoreIndexerService storeIndexerService;

  @Mock private StoreSyncWatermarkStore watermarkStore;

  @InjectMocks private StoreSyncScheduler scheduler;

  @Test
  @DisplayName("정상 워터마크 동기화 동작")
  void pollAndSyncStores_Success() {
    // given
    willDoNothing().given(storeIndexerService).ensureIndexExists();
    given(watermarkStore.getStoredWatermark())
        .willReturn(Optional.of(Instant.now().minusSeconds(300)));
    given(storeIndexerService.countIndexedStores()).willReturn(10L);
    willDoNothing()
        .given(storeIndexerService)
        .syncUpdatedStores(any(Instant.class), any(Instant.class));

    // when
    scheduler.pollAndSyncStores();

    // then
    verify(storeIndexerService).syncUpdatedStores(any(Instant.class), any(Instant.class));
    verify(watermarkStore).updateLastSyncedAt(any(Instant.class));
  }

  @Test
  @DisplayName("부트스트랩 조건(watermark 없음) 시 전체 백필 동작")
  void pollAndSyncStores_Bootstrap_NoWatermark() {
    // given
    // watermark가 없으면 isBootstrap = getStoredWatermark().isEmpty() || ... 에서
    // 단락 평가(short-circuit)로 countIndexedStores()는 호출되지 않는다 - stub하지 않는다.
    willDoNothing().given(storeIndexerService).ensureIndexExists();
    given(watermarkStore.getStoredWatermark()).willReturn(Optional.empty());
    willDoNothing()
        .given(storeIndexerService)
        .syncUpdatedStores(any(Instant.class), any(Instant.class));

    // when
    scheduler.pollAndSyncStores();

    // then
    verify(storeIndexerService).syncUpdatedStores(any(Instant.class), any(Instant.class));
    verify(watermarkStore).updateLastSyncedAt(any(Instant.class));
  }

  @Test
  @DisplayName("부트스트랩 조건(watermark는 있으나 ES 문서 0건) 시 전체 백필 동작")
  void pollAndSyncStores_Bootstrap_EmptyIndex() {
    // given
    // watermark는 존재하지만 ES 문서가 0건이면 두 번째 조건에서 countIndexedStores()가 실제로 호출되어
    // isBootstrap이 true가 되는 경로를 검증한다.
    willDoNothing().given(storeIndexerService).ensureIndexExists();
    given(watermarkStore.getStoredWatermark())
        .willReturn(Optional.of(Instant.now().minusSeconds(300)));
    given(storeIndexerService.countIndexedStores()).willReturn(0L);
    willDoNothing()
        .given(storeIndexerService)
        .syncUpdatedStores(any(Instant.class), any(Instant.class));

    // when
    scheduler.pollAndSyncStores();

    // then
    verify(storeIndexerService).syncUpdatedStores(any(Instant.class), any(Instant.class));
    verify(watermarkStore).updateLastSyncedAt(any(Instant.class));
  }
}
