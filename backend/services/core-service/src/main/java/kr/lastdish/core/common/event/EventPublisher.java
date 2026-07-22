package kr.lastdish.core.common.event;

/**
 * Outbox에 저장된 직렬화 이벤트를 외부 전달 기술로 발행하는 포트입니다.
 *
 * <p>구현체는 Spring Event, Kafka 등 발행 기술에 맞게 EventMessage를 처리합니다.
 */
public interface EventPublisher {

  /**
   * 직렬화된 이벤트 메시지를 현재 활성화된 발행 기술로 전달합니다.
   *
   * <p>발행에 실패하면 RuntimeException을 발생시켜 Processor가 Outbox를 PUBLISHED로 변경하지 않게 해야 합니다.
   *
   * @param message 발행할 이벤트 메시지
   */
  void publish(EventMessage message);
}
