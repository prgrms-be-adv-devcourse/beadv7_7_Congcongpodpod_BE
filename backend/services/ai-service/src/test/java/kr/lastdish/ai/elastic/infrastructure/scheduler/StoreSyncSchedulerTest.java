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
  @DisplayName("부트스트랩 조건 시 전체 백필 동작")
  void pollAndSyncStores_Bootstrap() {
    // given
    willDoNothing().given(storeIndexerService).ensureIndexExists();
    given(watermarkStore.getStoredWatermark()).willReturn(Optional.empty());
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
