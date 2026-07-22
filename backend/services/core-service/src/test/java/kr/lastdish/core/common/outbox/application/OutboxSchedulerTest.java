package kr.lastdish.core.common.outbox.application;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

  @Mock private OutboxClaimService claimService;

  @Mock private OutboxEventProcessor processor;

  @Mock private OutboxFailureRecorder failureRecorder;

  private OutboxScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new OutboxScheduler(claimService, processor, failureRecorder);
  }

  @Test
  void processes_claimed_events_in_order() {
    // given
    UUID firstEventId = UUID.randomUUID();
    UUID secondEventId = UUID.randomUUID();

    when(claimService.claim()).thenReturn(List.of(firstEventId, secondEventId));

    // when
    scheduler.publishEvents();

    // then
    InOrder inOrder = Mockito.inOrder(processor);

    inOrder.verify(processor).process(firstEventId);
    inOrder.verify(processor).process(secondEventId);
  }

  @Test
  void records_failure_and_continues_with_next_event() {
    // given
    UUID failedEventId = UUID.randomUUID();
    UUID nextEventId = UUID.randomUUID();

    RuntimeException processException = new RuntimeException("이벤트 발행 실패");

    when(claimService.claim()).thenReturn(List.of(failedEventId, nextEventId));

    doThrow(processException).when(processor).process(failedEventId);

    // when
    scheduler.publishEvents();

    // then
    verify(failureRecorder).record(failedEventId, processException);

    /*
     * 첫 번째 이벤트가 실패해도 다음 이벤트가 처리되는지 검증합니다.
     */
    verify(processor).process(nextEventId);
  }

  @Test
  void continues_processing_when_failure_recording_also_fails() {
    // given
    UUID failedEventId = UUID.randomUUID();
    UUID nextEventId = UUID.randomUUID();

    RuntimeException processException = new RuntimeException("이벤트 발행 실패");

    when(claimService.claim()).thenReturn(List.of(failedEventId, nextEventId));

    doThrow(processException).when(processor).process(failedEventId);

    doThrow(new RuntimeException("실패 기록 저장 실패"))
        .when(failureRecorder)
        .record(failedEventId, processException);

    // when
    scheduler.publishEvents();

    // then
    verify(processor).process(nextEventId);
  }
}
