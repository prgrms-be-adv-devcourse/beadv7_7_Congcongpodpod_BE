package kr.lastdish.core.common.outbox.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbox 도메인이 사용하는 저장소 계약입니다.
 *
 * <p>도메인 및 Application 계층이 Spring Data JPA에 직접 의존하지 않게 합니다.
 */
public interface OutboxEventRepository {

  /**
   * Outbox 이벤트를 저장합니다.
   */
  OutboxEvent save(OutboxEvent event);

  /**
   * eventId로 Outbox 이벤트를 조회합니다.
   */
  Optional<OutboxEvent> findById(UUID eventId);
}