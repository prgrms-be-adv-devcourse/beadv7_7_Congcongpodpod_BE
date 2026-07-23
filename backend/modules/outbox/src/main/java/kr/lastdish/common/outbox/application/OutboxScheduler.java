package kr.lastdish.common.outbox.application;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 일정 주기로 발행 가능한 Outbox 이벤트를 조회하고 처리합니다.
 *
 * <p>사용자 API 요청에서는 Outbox 저장까지만 수행하고, 실제 이벤트 발행은 이 Scheduler가 처리합니다. 따라서 API 요청과 이벤트 소비가 비동기로
 * 분리됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.scheduler.enabled", havingValue = "true")
public class OutboxScheduler {

  private final OutboxClaimService claimService;
  private final OutboxEventProcessor processor;
  private final OutboxFailureRecorder failureRecorder;

  /**
   * 이전 실행이 끝난 후 설정된 시간만큼 기다렸다가 다시 실행합니다.
   *
   * <p>fixedRate가 아니라 fixedDelay를 사용하여 이전 작업이 끝나기 전에 같은 Scheduler 작업이 다시 시작되는 것을 방지합니다.
   */
  @Scheduled(fixedDelayString = "${outbox.polling-delay-ms:1000}")
  public void publishEvents() {
    List<UUID> eventIds = claimService.claim();

    for (UUID eventId : eventIds) {
      processEvent(eventId);
    }
  }

  /**
   * 이벤트 한 건을 처리하고 실패 정보를 기록합니다.
   *
   * <p>한 이벤트가 실패해도 예외를 Scheduler 밖으로 전달하지 않아 같은 배치의 다음 이벤트를 계속 처리할 수 있게 합니다.
   */
  private void processEvent(UUID eventId) {
    try {
      processor.process(eventId);
    } catch (Exception processException) {
      log.error("Outbox 이벤트 발행에 실패했습니다. eventId={}", eventId, processException);

      recordFailure(eventId, processException);
    }
  }

  /**
   * 처리 실패 기록 자체가 실패하더라도 다음 이벤트 처리를 계속합니다.
   *
   * <p>실패 기록 DB 작업까지 예외를 전파하면 같은 배치의 나머지 이벤트가 처리되지 않을 수 있으므로 별도로 예외를 처리합니다.
   */
  private void recordFailure(UUID eventId, Exception processException) {
    try {
      failureRecorder.record(eventId, processException);
    } catch (Exception recordException) {
      log.error("Outbox 이벤트 실패 기록에 실패했습니다. eventId={}", eventId, recordException);
    }
  }
}
