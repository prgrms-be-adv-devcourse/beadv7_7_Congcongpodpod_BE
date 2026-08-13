package kr.lastdish.common.inbox.domain;

import jakarta.persistence.*;
import java.time.Instant;
import kr.lastdish.common.event.EventMessage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "inbox_events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboxEvent {

  @EmbeddedId private InboxEventId id;

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "aggregate_type", nullable = false, length = 50)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private Long aggregateId;

  @Column(name = "aggregate_version", nullable = false)
  private long aggregateVersion;

  @Column(name = "schema_version", nullable = false)
  private int schemaVersion;

  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private InboxStatus status;

  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  @Column(name = "last_error", length = 1000)
  private String lastError;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  @Column(name = "locked_at")
  private Instant lockedAt;

  @Column(name = "processed_at")
  private Instant processedAt;

  public static InboxEvent received(String consumerId, EventMessage message, Instant receivedAt) {
    InboxEvent inbox = new InboxEvent();
    inbox.id = new InboxEventId(consumerId, message.eventId());
    inbox.eventType = message.eventType();
    inbox.aggregateType = message.aggregateType();
    inbox.aggregateId = message.aggregateId();
    inbox.aggregateVersion = message.aggregateVersion();
    inbox.schemaVersion = message.schemaVersion();
    inbox.payload = message.payload();
    inbox.status = InboxStatus.RECEIVED;
    inbox.retryCount = 0;
    inbox.occurredAt = message.occurredAt();
    inbox.receivedAt = receivedAt;
    return inbox;
  }

  public void markProcessing(Instant lockedAt) {
    status = InboxStatus.PROCESSING;
    this.lockedAt = lockedAt;
  }

  public void markProcessed(Instant processedAt) {
    status = InboxStatus.PROCESSED;
    this.processedAt = processedAt;
    lockedAt = null;
    lastError = null;
  }

  public void markSkipped(String reason, Instant processedAt) {
    status = InboxStatus.SKIPPED;
    this.processedAt = processedAt;
    lockedAt = null;
    lastError = truncate(reason, 1000);
  }

  public void recordFailure(String error, int maxRetries) {
    retryCount++;
    lastError = truncate(error, 1000);
    lockedAt = null;
    status = retryCount >= maxRetries ? InboxStatus.FAILED : InboxStatus.RECEIVED;
  }

  public EventMessage toEventMessage() {
    return new EventMessage(
        id.getEventId(),
        eventType,
        aggregateType,
        aggregateId,
        aggregateVersion,
        schemaVersion,
        payload,
        occurredAt);
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
