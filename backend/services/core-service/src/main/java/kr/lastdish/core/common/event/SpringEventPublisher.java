package kr.lastdish.core.common.event;

import kr.lastdish.core.common.outbox.infrastructure.OutboxEventSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "event.publisher", havingValue = "spring", matchIfMissing = true)
public class SpringEventPublisher implements EventPublisher {

  private final ApplicationEventPublisher applicationEventPublisher;
  private final OutboxEventSerializer serializer;

  @Override
  public void publish(EventMessage message) {
    DomainEvent event = serializer.deserialize(message.eventType(), message.payload());

    applicationEventPublisher.publishEvent(event);
  }
}
