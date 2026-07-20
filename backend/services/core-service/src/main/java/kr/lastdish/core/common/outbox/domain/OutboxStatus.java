package kr.lastdish.core.common.outbox.domain;

/** Outbox 이벤트의 처리 상태입니다. */
public enum OutboxStatus {

  /** 아직 발행되지 않은 이벤트입니다. */
  PENDING,

  /** Scheduler 또는 Processor가 발행을 위해 선점한 상태입니다. */
  PROCESSING,

  /**
   * EventPublisher에 이벤트 전달이 성공한 상태입니다.
   *
   * <p>Kafka 전환 후에는 Kafka Broker가 이벤트를 확인한 상태를 의미하며, Consumer의 처리 완료를 의미하지는 않습니다.
   */
  PUBLISHED,

  /** 최대 재시도 횟수를 초과하여 자동 처리를 중단한 상태입니다. */
  FAILED
}
