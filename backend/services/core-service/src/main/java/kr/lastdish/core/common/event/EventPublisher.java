package kr.lastdish.core.common.event;

/**
 * 도메인 이벤트를 외부 전달 기술로 발행하는 포트입니다.
 *
 * <p>Outbox Processor는 Spring Event나 Kafka 같은 구체적인 기술을 직접 알지 않고 이 인터페이스에만 의존합니다.
 *
 * <p>향후 Kafka를 도입하면 이 인터페이스를 구현하는 KafkaEventPublisher를 추가하여 발행 기술을 교체할 수 있습니다.
 */
public interface EventPublisher {

  /**
   * 도메인 이벤트를 현재 활성화된 발행 기술로 전달합니다.
   *
   * <p>발행에 실패하면 RuntimeException을 발생시켜 Outbox Processor가 해당 이벤트를 PUBLISHED로 변경하지 않고 재시도할 수 있게 해야
   * 합니다.
   *
   * @param event 발행할 도메인 이벤트
   */
  void publish(DomainEvent event);
}
