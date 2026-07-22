package kr.lastdish.core.common.event.spring;

import kr.lastdish.core.common.event.DomainEvent;

/**
 * 이벤트 타입 문자열과 Spring Event Java 클래스를 연결하는 계약입니다.
 *
 * <p>각 도메인은 자신의 이벤트 Resolver를 구현해 등록합니다. 공통 Spring 이벤트 인프라는 구체적인 도메인 이벤트 클래스를 직접 참조하지 않습니다.
 */
public interface SpringEventTypeResolver {

  /** Outbox의 eventType과 비교할 이벤트 타입을 반환합니다. */
  String eventType();

  /** payload를 역직렬화할 Java 이벤트 클래스를 반환합니다. */
  Class<? extends DomainEvent> eventClass();
}
