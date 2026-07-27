package kr.lastdish.common.outbox.infrastructure;

import java.util.Optional;
import java.util.UUID;
import kr.lastdish.common.outbox.domain.OutboxEvent;
import kr.lastdish.common.outbox.domain.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** OutboxEventRepository 계약을 Spring Data JPA로 구현하는 Adapter입니다. */
@Repository
@RequiredArgsConstructor
public class OutboxRepositoryAdapter implements OutboxEventRepository {

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  private final OutboxJpaRepository jpaRepository;

  @Override
  public OutboxEvent save(OutboxEvent event) {
    return jpaRepository.save(event);
  }

  @Override
  public Optional<OutboxEvent> findById(UUID eventId) {
    return jpaRepository.findById(eventId);
  }
}
