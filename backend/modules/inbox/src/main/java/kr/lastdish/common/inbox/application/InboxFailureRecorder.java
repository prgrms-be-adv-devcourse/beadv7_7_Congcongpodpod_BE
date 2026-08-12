package kr.lastdish.common.inbox.application;

import kr.lastdish.common.inbox.domain.InboxEvent;
import kr.lastdish.common.inbox.domain.InboxEventId;
import kr.lastdish.common.inbox.domain.InboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxFailureRecorder {

  private final InboxEventRepository repository;
  private final int maxRetries;

  public InboxFailureRecorder(
      InboxEventRepository repository, @Value("${inbox.max-retries:5}") int maxRetries) {
    this.repository = repository;
    this.maxRetries = maxRetries;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(InboxEventId id, Exception exception) {
    InboxEvent inbox = repository.findById(id).orElseThrow();

    String error =
        exception.getMessage() != null
            ? exception.getMessage()
            : exception.getClass().getSimpleName();

    inbox.recordFailure(error, maxRetries);
  }
}
