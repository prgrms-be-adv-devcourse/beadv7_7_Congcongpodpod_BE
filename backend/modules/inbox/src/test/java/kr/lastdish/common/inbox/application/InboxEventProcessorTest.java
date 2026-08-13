package kr.lastdish.common.inbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxAggregateVersion;
import kr.lastdish.common.inbox.domain.InboxAggregateVersionId;
import kr.lastdish.common.inbox.domain.InboxAggregateVersionRepository;
import kr.lastdish.common.inbox.domain.InboxEvent;
import kr.lastdish.common.inbox.domain.InboxEventHandler;
import kr.lastdish.common.inbox.domain.InboxEventId;
import kr.lastdish.common.inbox.domain.InboxEventRepository;
import kr.lastdish.common.inbox.domain.InboxProcessingPolicy;
import kr.lastdish.common.inbox.domain.InboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboxEventProcessorTest {

  private static final String CONSUMER_ID = "cart-dish-state";
  private static final String EVENT_TYPE = "DISH_STATE_CHANGED";
  private static final String AGGREGATE_TYPE = "DISH";
  private static final long AGGREGATE_ID = 1L;
  private static final Instant NOW = Instant.parse("2026-08-12T00:00:00Z");

  @Mock private InboxEventRepository repository;
  @Mock private InboxEventHandlerRegistry registry;
  @Mock private InboxAggregateVersionRepository aggregateVersionRepository;
  @Mock private InboxEventHandler handler;

  private InboxEventProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new InboxEventProcessor(repository, registry, aggregateVersionRepository);
  }

  @Test
  void 최신값우선_정책은_이미_적용한_버전_이하를_건너뛴다() {
    InboxEvent inbox = processingInbox(7L);
    InboxAggregateVersion progress = progressAt(8L);
    prepare(inbox, InboxProcessingPolicy.IDEMPOTENT_LATEST_WINS);
    when(aggregateVersionRepository.getOrCreateAndLock(any(), any())).thenReturn(progress);

    processor.process(inbox.getId());

    assertThat(inbox.getStatus()).isEqualTo(InboxStatus.SKIPPED);
    assertThat(inbox.getLastError()).isEqualTo("OLDER_THAN_LAST_APPLIED");
    verify(handler, never()).handle(any());
  }

  @Test
  void 최신값우선_정책은_새로운_버전을_처리하고_진행값을_갱신한다() {
    InboxEvent inbox = processingInbox(8L);
    InboxAggregateVersion progress = progressAt(6L);
    prepare(inbox, InboxProcessingPolicy.IDEMPOTENT_LATEST_WINS);
    when(aggregateVersionRepository.getOrCreateAndLock(any(), any())).thenReturn(progress);

    processor.process(inbox.getId());

    verify(handler).handle(inbox.toEventMessage());
    assertThat(inbox.getStatus()).isEqualTo(InboxStatus.PROCESSED);
    assertThat(progress.getLastProcessedVersion()).isEqualTo(8L);
  }

  @Test
  void 멱등_정책은_버전_진행값을_조회하지_않고_처리한다() {
    InboxEvent inbox = processingInbox(1L);
    prepare(inbox, InboxProcessingPolicy.IDEMPOTENT);

    processor.process(inbox.getId());

    verify(handler).handle(inbox.toEventMessage());
    verify(aggregateVersionRepository, never()).getOrCreateAndLock(any(), any());
    assertThat(inbox.getStatus()).isEqualTo(InboxStatus.PROCESSED);
  }

  private void prepare(InboxEvent inbox, InboxProcessingPolicy policy) {
    InboxEventId id = inbox.getId();
    when(repository.findById(id)).thenReturn(Optional.of(inbox));
    when(registry.get(CONSUMER_ID, EVENT_TYPE)).thenReturn(handler);
    when(handler.processingPolicy()).thenReturn(policy);
  }

  private InboxEvent processingInbox(long aggregateVersion) {
    EventMessage message =
        new EventMessage(
            UUID.randomUUID(),
            EVENT_TYPE,
            AGGREGATE_TYPE,
            AGGREGATE_ID,
            aggregateVersion,
            1,
            "{}",
            NOW);
    InboxEvent inbox = InboxEvent.received(CONSUMER_ID, message, NOW);
    inbox.markProcessing(NOW);
    return inbox;
  }

  private InboxAggregateVersion progressAt(long version) {
    InboxAggregateVersion progress =
        InboxAggregateVersion.initial(
            new InboxAggregateVersionId(CONSUMER_ID, AGGREGATE_TYPE, AGGREGATE_ID), NOW);
    if (version > 0) {
      progress.advanceLatestTo(version, NOW);
    }
    return progress;
  }
}
