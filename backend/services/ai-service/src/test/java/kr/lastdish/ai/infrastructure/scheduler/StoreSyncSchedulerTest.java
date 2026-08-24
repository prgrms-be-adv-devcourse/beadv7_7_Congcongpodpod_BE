package kr.lastdish.ai.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import kr.lastdish.ai.elastic.application.StoreIndexerService;
import kr.lastdish.ai.elastic.infrastructure.scheduler.StoreSyncScheduler;
import kr.lastdish.ai.elastic.infrastructure.scheduler.StoreSyncWatermarkStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreSyncSchedulerTest {

  @Mock private StoreIndexerService storeIndexerService;
  @Mock private StoreSyncWatermarkStore watermarkStore;

  @InjectMocks private StoreSyncScheduler scheduler;

  @Test
  @DisplayName("동기화 성공 시 watermark가 to 시각으로 전진한다")
  void pollAndSyncStores_success_advancesWatermark() {
    // given
    Instant lastSynced = Instant.parse("2026-08-24T10:00:00Z");
    given(watermarkStore.getLastSyncedAt()).willReturn(lastSynced);

    // when
    scheduler.pollAndSyncStores();

    // then
    ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(storeIndexerService).syncUpdatedStores(any(Instant.class), toCaptor.capture());
    verify(watermarkStore).updateLastSyncedAt(toCaptor.getValue());
  }

  @Test
  @DisplayName("watermark보다 10초 이전(overlap)부터 조회 구간이 시작된다")
  void pollAndSyncStores_appliesOverlapSeconds() {
    // given
    Instant lastSynced = Instant.parse("2026-08-24T10:00:00Z");
    given(watermarkStore.getLastSyncedAt()).willReturn(lastSynced);

    // when
    scheduler.pollAndSyncStores();

    // then
    ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(storeIndexerService).syncUpdatedStores(fromCaptor.capture(), any(Instant.class));
    assertThat(fromCaptor.getValue()).isEqualTo(lastSynced.minusSeconds(10));
  }

  @Test
  @DisplayName("동기화 중 예외가 발생하면 watermark가 갱신되지 않는다")
  void pollAndSyncStores_failure_doesNotAdvanceWatermark() {
    // given
    given(watermarkStore.getLastSyncedAt()).willReturn(Instant.now());
    willThrow(new RuntimeException("Core API 장애"))
        .given(storeIndexerService)
        .syncUpdatedStores(any(), any());

    // when & then: 예외가 밖으로 새어나가지 않아야 스케줄러가 안 죽음
    assertThatCode(() -> scheduler.pollAndSyncStores()).doesNotThrowAnyException();
    verify(watermarkStore, never()).updateLastSyncedAt(any());
  }

  @Test
  @DisplayName("watermark 조회 자체가 실패해도 스케줄러가 죽지 않고, 동기화는 시도되지 않는다")
  void pollAndSyncStores_watermarkReadFailure_doesNotThrow() {
    // given
    given(watermarkStore.getLastSyncedAt()).willThrow(new RuntimeException("Redis 장애"));

    // when & then
    assertThatCode(() -> scheduler.pollAndSyncStores()).doesNotThrowAnyException();
    verify(storeIndexerService, never()).syncUpdatedStores(any(), any());
    verify(watermarkStore, never()).updateLastSyncedAt(any());
  }

  @Test
  @DisplayName("임베딩 재시도 스캔이 성공적으로 위임된다")
  void retryFailedEmbeddings_delegatesToService() {
    // when
    scheduler.retryFailedEmbeddings();

    // then
    verify(storeIndexerService).retryFailedEmbeddings();
  }

  @Test
  @DisplayName("임베딩 재시도 스캔 중 예외가 발생해도 스케줄러가 죽지 않는다")
  void retryFailedEmbeddings_failure_doesNotThrow() {
    // given
    willThrow(new RuntimeException("ES 장애")).given(storeIndexerService).retryFailedEmbeddings();

    // when & then
    assertThatCode(() -> scheduler.retryFailedEmbeddings()).doesNotThrowAnyException();
  }
}
