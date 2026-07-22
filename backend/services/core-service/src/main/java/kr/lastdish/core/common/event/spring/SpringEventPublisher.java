package kr.lastdish.core.common.event.spring;


import kr.lastdish.core.common.event.EventMessage;
import kr.lastdish.core.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "event.publisher",
    havingValue = "spring",
    matchIfMissing = true)
public class SpringEventPublisher implements EventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  @Override
  public void publish(EventMessage message) {
    applicationEventPublisher.publishEvent(message);
  }
}
