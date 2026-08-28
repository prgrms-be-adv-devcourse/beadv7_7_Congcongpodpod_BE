package kr.lastdish.ai.elastic.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import kr.lastdish.ai.elastic.application.StoreIndexerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbeddingRetrySchedulerTest {

  @Mock private StoreIndexerService storeIndexerService;

  @InjectMocks private EmbeddingRetryScheduler scheduler;

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
