package kr.lastdish.core.dish.infrastructure.event;

import kr.lastdish.core.common.event.DomainEvent;
import kr.lastdish.core.common.event.spring.SpringEventTypeResolver;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import org.springframework.stereotype.Component;

/** DishStateChangedEvent의 eventType과 Java 클래스를 Spring 이벤트 인프라에 등록합니다. */
@Component
public class DishStateChangedSpringEventTypeResolver implements SpringEventTypeResolver {

  @Override
  public String eventType() {
    return DishStateChangedEvent.EVENT_TYPE;
  }

  @Override
  public Class<? extends DomainEvent> eventClass() {
    return DishStateChangedEvent.class;
  }
}
