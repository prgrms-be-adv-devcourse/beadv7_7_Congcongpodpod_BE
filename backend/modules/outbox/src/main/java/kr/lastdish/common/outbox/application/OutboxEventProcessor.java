package kr.lastdish.common.outbox.application;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.event.EventPublisher;
import kr.lastdish.common.outbox.domain.OutboxEvent;
import kr.lastdish.common.outbox.domain.OutboxEventRepository;
import kr.lastdish.common.outbox.domain.OutboxStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 선점된 Outbox 이벤트 한 건을 실제 이벤트로 발행합니다.
 *
 * <p>각 이벤트를 독립된 트랜잭션으로 처리하여 한 이벤트의 발행 실패가 같은 배치에 포함된 다른 이벤트 처리까지 롤백시키지 않게 합니다.
 */
@Service
@RequiredArgsConstructor
public class OutboxEventProcessor {

  private final OutboxEventRepository repository;
  private final EventPublisher eventPublisher;

  /**
   * PROCESSING 상태의 Outbox 이벤트 한 건을 발행합니다.
   *
   * <p>발행 성공 시 PUBLISHED 상태로 변경합니다. 역직렬화 또는 이벤트 발행이 실패하면 예외를 호출자에게 전달하며 현재 처리 트랜잭션은 롤백됩니다.
   *
   * @param eventId 발행할 Outbox 이벤트 식별자
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void process(UUID eventId) {
    OutboxEvent outbox =
        repository
            .findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Outbox 이벤트를 찾을 수 없습니다: " + eventId));

    /*
     * Scheduler가 정상적으로 선점한 PROCESSING 이벤트만 발행합니다.
     * PENDING 또는 이미 PUBLISHED된 이벤트의 잘못된 발행을 방지합니다.
     */
    validateProcessingStatus(outbox);

    EventMessage message =
        new EventMessage(
            outbox.getEventId(),
            outbox.getEventType(),
            outbox.getAggregateType(),
            outbox.getAggregateId(),
            outbox.getAggregateVersion(),
            outbox.getSchemaVersion(),
            outbox.getPayload(),
            outbox.getOccurredAt());

    /*
     * 현재는 Spring Event로 발행합니다.
     *
     * Spring Listener에서 예외가 발생하면 이 메서드까지 예외가 전파되며
     * 아래 markPublished()는 실행되지 않습니다.
     */
    eventPublisher.publish(message);

    /*
     * EventPublisher 호출이 정상적으로 끝난 경우에만 발행 완료를 기록합니다.
     */
    outbox.markPublished(Instant.now());
  }

  /** 선점되지 않은 Outbox 이벤트가 발행되는 것을 방지합니다. */
  private void validateProcessingStatus(OutboxEvent outbox) {
    if (outbox.getStatus() == OutboxStatus.PROCESSING) {
      return;
    }

    throw new IllegalStateException("PROCESSING 상태의 이벤트만 발행할 수 있습니다: " + outbox.getEventId());
  }
}
