package kr.lastdish.common.inbox.application;

import kr.lastdish.common.inbox.domain.InboxEventId;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inbox.scheduler.enabled", havingValue = "true")
public class InboxScheduler {
  private final InboxClaimService claimService;
  private final InboxEventProcessor processor;
  private final InboxFailureRecorder failureRecorder;

  @Scheduled(fixedDelayString = "${inbox.polling-delay-ms:1000}")
  public void processEvents() {
    for (InboxEventId id : claimService.claim()) {
      try {
        processor.process(id);
      } catch (Exception exception) {
        failureRecorder.record(id, exception);
      }
    }
  }
}
