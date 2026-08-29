package kr.lastdish.common.inbox.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 보관기간이 지난 Inbox 완료 이벤트를 설정된 주기마다 한 배치씩 정리합니다. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inbox.cleanup.enabled", havingValue = "true")
public class InboxCleanupScheduler {

  private final InboxCleanupService cleanupService;

  @Scheduled(fixedDelayString = "${inbox.cleanup.fixed-delay-ms:60000}")
  public void cleanup() {
    try {
      int deletedCount = cleanupService.cleanup();

      if (deletedCount > 0) {
        log.info("Inbox 완료 이벤트를 정리했습니다. deletedCount={}", deletedCount);
      }
    } catch (Exception exception) {
      log.error("Inbox 완료 이벤트 정리에 실패했습니다.", exception);
    }
  }
}
