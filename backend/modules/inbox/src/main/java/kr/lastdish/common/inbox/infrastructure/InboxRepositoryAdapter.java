package kr.lastdish.common.inbox.infrastructure;

import java.util.Optional;
import kr.lastdish.common.inbox.domain.InboxEvent;
import kr.lastdish.common.inbox.domain.InboxEventId;
import kr.lastdish.common.inbox.domain.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InboxRepositoryAdapter implements InboxEventRepository {

  private final InboxJpaRepository jpaRepository;

  @Override
  public InboxEvent save(InboxEvent event) {
    return jpaRepository.save(event);
  }

  @Override
  public Optional<InboxEvent> findById(InboxEventId id) {
    return jpaRepository.findById(id);
  }

  @Override
  public boolean existsById(InboxEventId id) {
    return jpaRepository.existsById(id);
  }

  @Override
  public void flush() {
    jpaRepository.flush();
  }
}
