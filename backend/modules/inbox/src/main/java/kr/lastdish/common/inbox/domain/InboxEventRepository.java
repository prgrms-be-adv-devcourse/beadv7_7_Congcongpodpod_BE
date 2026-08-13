package kr.lastdish.common.inbox.domain;

import java.util.Optional;

/**
 * Inbox 도메인이 사용하는 저장소 계약입니다.
 *
 * <p>도메인 및 Application 계층이 Spring Data JPA에 직접 의존하지 않게 합니다.
 */
public interface InboxEventRepository {

  InboxEvent save(InboxEvent event);

  Optional<InboxEvent> findById(InboxEventId id);

  boolean existsById(InboxEventId id);

  void flush();
}
