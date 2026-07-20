package kr.lastdish.core.common.event;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Spring Application Event를 사용하는 EventPublisher 구현체입니다.
 *
 * <p>기본 Spring Event는 이벤트를 발행한 스레드에서 Listener를 동기 실행합니다. 따라서 Listener가 예외를 발생시키면 publish 메서드에도 예외가
 * 전파됩니다.
 *
 * <p>Outbox Processor는 이 예외를 감지하여 발행 성공 상태인 PUBLISHED를 기록하지 않고 이벤트가 재시도되도록 처리합니다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.publisher", havingValue = "spring", matchIfMissing = true)
public class SpringEventPublisher implements EventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  /**
   * Spring Application Context에 도메인 이벤트를 발행합니다.
   *
   * @param event 발행할 도메인 이벤트
   */
  @Override
  public void publish(DomainEvent event) {
    applicationEventPublisher.publishEvent(event);
  }
}
