package kr.lastdish.common.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import kr.lastdish.common.outbox.domain.OutboxEvent;
import kr.lastdish.common.outbox.domain.OutboxEventRepository;
import kr.lastdish.common.outbox.domain.OutboxStatus;
import kr.lastdish.common.outbox.support.DishStateChangedEvent;
import kr.lastdish.common.outbox.support.DishStateChangedPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxFailureRecorderTest {

  @Mock private OutboxEventRepository repository;

  private OutboxFailureRecorder failureRecorder;

  @BeforeEach
  void setUp() {
    /*
     * 테스트에서는 최대 재시도 횟수를 2회로 설정합니다.
     */
    failureRecorder = new OutboxFailureRecorder(repository, 2);
  }

  @Test
  void returns_event_to_pending_when_retry_is_available() {
    // given
    OutboxEvent outbox = createProcessingOutbox();

    when(repository.findById(outbox.getEventId())).thenReturn(Optional.of(outbox));

    // when
    failureRecorder.record(outbox.getEventId(), new RuntimeException("일시적인 발행 오류"));

    // then
    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
    assertThat(outbox.getRetryCount()).isEqualTo(1);
    assertThat(outbox.getLastError()).isEqualTo("일시적인 발행 오류");
    assertThat(outbox.getLockedAt()).isNull();
  }

  @Test
  void marks_event_as_failed_when_max_retries_is_reached() {
    // given
    OutboxEvent outbox = createProcessingOutbox();

    when(repository.findById(outbox.getEventId())).thenReturn(Optional.of(outbox));

    // when
    failureRecorder.record(outbox.getEventId(), new RuntimeException("첫 번째 실패"));

    /*
     * 실제 Scheduler에서는 PENDING 이벤트를 다시 선점한 뒤 처리합니다.
     * 테스트에서도 재선점 상태를 표현합니다.
     */
    outbox.markProcessing(Instant.now());

    failureRecorder.record(outbox.getEventId(), new RuntimeException("두 번째 실패"));

    // then
    assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
    assertThat(outbox.getRetryCount()).isEqualTo(2);
    assertThat(outbox.getLastError()).isEqualTo("두 번째 실패");
  }

  private OutboxEvent createProcessingOutbox() {
    DishStateChangedPayload payload = new DishStateChangedPayload(false, 5L);

    DishStateChangedEvent event =
        new DishStateChangedEvent(
            UUID.randomUUID(),
            DishStateChangedEvent.SCHEMA_VERSION,
            1L,
            1L,
            payload,
            Instant.now());

    OutboxEvent outbox = OutboxEvent.create(event, "{}");

    outbox.markProcessing(Instant.now());

    return outbox;
  }
}
