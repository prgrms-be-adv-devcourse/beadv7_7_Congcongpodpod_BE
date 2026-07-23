package kr.lastdish.core.common.outbox.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import kr.lastdish.core.common.event.DomainEvent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 발행할 도메인 이벤트를 DB에 저장하는 Outbox 엔티티입니다.
 *
 * <p>도메인 변경과 OutboxEvent 저장을 같은 DB 트랜잭션에서 처리하여 도메인 변경은 커밋됐지만 이벤트 기록은 유실되는 상황을 방지합니다.
 */
@Getter
@Entity
@Table(
    name = "outbox_events",
    indexes = {@Index(name = "idx_outbox_status_occurred_at", columnList = "status, occurred_at")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

  /**
   * 이벤트의 고유 식별자입니다.
   *
   * <p>발행을 재시도해도 동일한 eventId를 유지합니다.
   */
  @Id
  @Column(name = "event_id", nullable = false)
  private UUID eventId;

  /** 저장된 payload를 어떤 이벤트 타입으로 역직렬화할지 결정합니다. */
  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  /**
   * 이벤트가 발생한 Aggregate 종류입니다.
   *
   * <p>예: DISH, STORE, ORDER
   */
  @Column(name = "aggregate_type", nullable = false, length = 50)
  private String aggregateType;

  /** 이벤트가 발생한 Aggregate 식별자입니다. */
  @Column(name = "aggregate_id", nullable = false)
  private Long aggregateId;

  /**
   * 동일 Aggregate에서 발생한 이벤트의 순서입니다.
   *
   * <p>기존 데이터에는 0을 적용하고 신규 이벤트는 Producer가 증가시킨 값을 저장합니다.
   */
  @Column(name = "aggregate_version", nullable = false, columnDefinition = "BIGINT DEFAULT 0")
  private long aggregateVersion;

  /**
   * 이벤트 payload 계약의 스키마 버전입니다.
   *
   * <p>Consumer가 지원하는 payload 구조인지 판단할 때 사용합니다.
   */
  @Column(name = "schema_version", nullable = false, columnDefinition = "INTEGER DEFAULT 1")
  private int schemaVersion;

  /** DomainEvent를 JSON으로 직렬화한 값입니다. */
  @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
  private String payload;

  /** 현재 Outbox 이벤트 처리 상태입니다. */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private OutboxStatus status;

  /** 이벤트 발행에 실패한 횟수입니다. */
  @Column(name = "retry_count", nullable = false)
  private int retryCount;

  /**
   * 마지막 발행 실패 원인입니다.
   *
   * <p>DB에 지나치게 큰 예외 메시지가 저장되지 않도록 길이를 제한합니다.
   */
  @Column(name = "last_error", length = 1000)
  private String lastError;

  /** 비즈니스 이벤트가 실제로 발생한 시각입니다. */
  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  /**
   * Processor가 이벤트를 선점한 시각입니다.
   *
   * <p>PROCESSING 상태에서 서버가 종료된 이벤트를 복구할 때 사용합니다.
   */
  @Column(name = "locked_at")
  private Instant lockedAt;

  /** 이벤트 발행이 성공한 시각입니다. */
  @Column(name = "published_at")
  private Instant publishedAt;

  /**
   * DomainEvent와 직렬화된 payload로 새로운 OutboxEvent를 생성합니다.
   *
   * <p>새 이벤트는 아직 발행되지 않았으므로 PENDING 상태로 생성합니다.
   */
  public static OutboxEvent create(DomainEvent<?> event, String payload) {

    OutboxEvent outbox = new OutboxEvent();

    outbox.eventId = event.eventId();
    outbox.eventType = event.eventType();
    outbox.aggregateType = event.aggregateType();
    outbox.aggregateId = event.aggregateId();
    outbox.aggregateVersion = event.aggregateVersion();
    outbox.schemaVersion = event.schemaVersion();
    outbox.payload = payload;
    outbox.status = OutboxStatus.PENDING;
    outbox.retryCount = 0;
    outbox.occurredAt = event.occurredAt();

    return outbox;
  }

  /** 이벤트를 발행 대상으로 선점합니다. */
  public void markProcessing(Instant lockedAt) {
    this.status = OutboxStatus.PROCESSING;
    this.lockedAt = lockedAt;
  }

  /** 이벤트 발행 성공을 기록합니다. */
  public void markPublished(Instant publishedAt) {
    this.status = OutboxStatus.PUBLISHED;
    this.publishedAt = publishedAt;

    // 발행 처리가 끝났으므로 선점 시각과 이전 오류를 제거합니다.
    this.lockedAt = null;
    this.lastError = null;
  }

  /**
   * 이벤트 발행 실패를 기록합니다.
   *
   * <p>최대 재시도 횟수에 도달하지 않았으면 PENDING으로 되돌리고, 최대 횟수에 도달했으면 FAILED로 변경합니다.
   */
  public void recordFailure(String errorMessage, int maxRetries) {
    this.retryCount++;
    this.lastError = truncate(errorMessage, 1000);
    this.lockedAt = null;

    this.status = retryCount >= maxRetries ? OutboxStatus.FAILED : OutboxStatus.PENDING;
  }

  /** DB 컬럼 최대 길이에 맞게 오류 메시지를 자릅니다. */
  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }

    return value.substring(0, maxLength);
  }
}
