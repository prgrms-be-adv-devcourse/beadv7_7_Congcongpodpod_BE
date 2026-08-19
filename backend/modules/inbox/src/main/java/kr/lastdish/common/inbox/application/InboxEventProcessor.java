package kr.lastdish.common.inbox.application;

import java.time.Instant;
import kr.lastdish.common.event.EventHandlerRegistry;
import kr.lastdish.common.inbox.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InboxEventProcessor {

  private final InboxEventRepository repository;
  private final EventHandlerRegistry registry;
  private final InboxAggregateVersionRepository aggregateVersionRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void process(InboxEventId id) {
    InboxEvent inbox = repository.findById(id).orElseThrow();

    if (inbox.getStatus() != InboxStatus.PROCESSING) {
      throw new IllegalStateException("PROCESSING Inbox만 처리할 수 있습니다: " + id);
    }

    if (!(registry.get(id.getConsumerId(), inbox.getEventType())
        instanceof InboxEventHandler handler)) {
      throw new IllegalStateException("InboxEventHandler만 처리할 수 있습니다: " + id.getConsumerId());
    }

    Instant now = Instant.now();

    switch (handler.processingPolicy()) {
      case IDEMPOTENT -> processIdempotent(inbox, handler, now);
      case IDEMPOTENT_LATEST_WINS -> processLatestWins(inbox, handler, now);
    }
  }

  private void processIdempotent(InboxEvent inbox, InboxEventHandler handler, Instant now) {
    handler.handle(inbox.toEventMessage());
    inbox.markProcessed(now);
  }

  private void processLatestWins(InboxEvent inbox, InboxEventHandler handler, Instant now) {
    InboxAggregateVersion progress =
        aggregateVersionRepository.getOrCreateAndLock(
            new InboxAggregateVersionId(
                inbox.getId().getConsumerId(), inbox.getAggregateType(), inbox.getAggregateId()),
            now);

    long incoming = inbox.getAggregateVersion();

    if (incoming <= progress.getLastProcessedVersion()) {
      inbox.markSkipped("OLDER_THAN_LAST_APPLIED", now);
      return;
    }

    handler.handle(inbox.toEventMessage());
    progress.advanceLatestTo(incoming, now);
    inbox.markProcessed(now);
  }
}
