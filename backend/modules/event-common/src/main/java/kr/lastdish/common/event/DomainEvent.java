package kr.lastdish.common.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Outbox 또는 메시지 브로커를 통해 전달할 도메인 이벤트의 공통 계약입니다.
 *
 * <p>각 도메인 이벤트는 이 인터페이스를 구현하여 이벤트 식별자, 스키마 버전, 이벤트 종류, Aggregate 정보 및 발생 시각을 일관된 형태로 제공합니다.
 *
 * <p>현재는 Spring Event로 발행하지만, 향후 Kafka로 전환하더라도 이벤트 계약은 그대로 유지할 수 있습니다.
 */
public interface DomainEvent<P> {

  /**
   * 이벤트를 고유하게 식별하는 값입니다.
   *
   * <p>동일한 이벤트가 재시도 또는 중복 전달될 때 Consumer가 중복 처리 여부를 판단하는 데 사용합니다.
   */
  UUID eventId();

  /**
   * 이벤트 payload의 스키마 버전입니다.
   *
   * <p>이벤트 필드가 변경될 때 Consumer와의 호환성을 관리하기 위해 사용합니다. 최초 버전은 1부터 시작합니다.
   */
  int schemaVersion();

  /**
   * 이벤트 종류를 나타내는 고정 문자열입니다.
   *
   * <p>Java 클래스 이름을 직접 사용하지 않고 고정된 문자열을 사용해야 클래스 이름이나 패키지 변경이 저장된 이벤트 해석에 영향을 주지 않습니다.
   */
  String eventType();

  /**
   * 이벤트가 발생한 Aggregate 종류입니다.
   *
   * <p>예: DISH, STORE, ORDER
   */
  String aggregateType();

  /**
   * 이벤트가 발생한 Aggregate 식별자입니다.
   *
   * <p>현재 Core Service 도메인의 기본 키 형식에 맞춰 Long을 사용합니다.
   */
  Long aggregateId();

  /**
   * 동일 Aggregate에서 발생한 상태 변경 순서입니다.
   *
   * <p>Consumer는 마지막으로 처리한 version 이하의 이벤트를 무시하여 과거 상태가 다시 반영되는 것을 방지할 수 있습니다.
   */
  long aggregateVersion();

  /**
   * 비즈니스 이벤트가 실제로 발생한 시각입니다.
   *
   * <p>Outbox 발행 시각이 아니라 Dish 상태 등이 변경된 시각입니다.
   */
  Instant occurredAt();

  P payload();
}
