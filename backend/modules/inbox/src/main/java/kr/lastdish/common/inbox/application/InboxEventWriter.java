package kr.lastdish.common.inbox.application;

import java.time.Instant;
import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxEvent;
import kr.lastdish.common.inbox.domain.InboxEventId;
import kr.lastdish.common.inbox.domain.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InboxEventWriter {

  private final InboxEventRepository repository;

  @Transactional
  public boolean saveIfAbsent(String consumerId, EventMessage message) {
    InboxEventId id = new InboxEventId(consumerId, message.eventId());

    if (repository.existsById(id)) {
      return false;
    }

    try {
      repository.save(InboxEvent.received(consumerId, message, Instant.now()));
      repository.flush();
      return true;
    } catch (DataIntegrityViolationException duplicate) {
      return false;
    }
  }
}
