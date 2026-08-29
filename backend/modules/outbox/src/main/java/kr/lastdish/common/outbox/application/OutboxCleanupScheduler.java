package kr.lastdish.common.outbox.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 발행 완료된 Outbox 이벤트를 설정된 주기마다 한 배치씩 정리합니다. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "outbox.cleanup.enabled", havingValue = "true")
public class OutboxCleanupScheduler {

  private final OutboxCleanupService cleanupService;

  @Scheduled(fixedDelayString = "${outbox.cleanup.fixed-delay-ms:60000}")
  public void cleanup() {
    try {
      int deletedCount = cleanupService.cleanup();

      if (deletedCount > 0) {
        log.info("Outbox 완료 이벤트를 정리했습니다. deletedCount={}", deletedCount);
      }
    } catch (Exception exception) {
      log.error("Outbox 완료 이벤트 정리에 실패했습니다.", exception);
    }
  }
}
