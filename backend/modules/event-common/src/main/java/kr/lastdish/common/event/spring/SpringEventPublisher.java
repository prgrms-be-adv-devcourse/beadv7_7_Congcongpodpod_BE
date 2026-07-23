package kr.lastdish.common.event.spring;

import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

@RequiredArgsConstructor
public class SpringEventPublisher implements EventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;

  @Override
  public void publish(EventMessage message) {
    applicationEventPublisher.publishEvent(message);
  }
}
