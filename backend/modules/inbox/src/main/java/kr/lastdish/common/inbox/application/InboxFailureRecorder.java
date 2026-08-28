package kr.lastdish.common.inbox.application;

import kr.lastdish.common.event.EventHandler;
import kr.lastdish.common.event.EventHandlerRegistry;
import kr.lastdish.common.inbox.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxFailureRecorder {

  private static final Logger log = LoggerFactory.getLogger(InboxFailureRecorder.class);

  private final InboxEventRepository repository;
  private final EventHandlerRegistry handlerRegistry;
  private final int maxRetries;

  public InboxFailureRecorder(
      InboxEventRepository repository,
      EventHandlerRegistry handlerRegistry,
      @Value("${inbox.max-retries:5}") int maxRetries) {
    this.repository = repository;
    this.handlerRegistry = handlerRegistry;
    this.maxRetries = maxRetries;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(InboxEventId id, Exception exception) {
    InboxEvent inbox = repository.findById(id).orElseThrow();

    String error =
        exception.getMessage() != null
            ? exception.getMessage()
            : exception.getClass().getSimpleName();

    boolean exhausted = inbox.recordFailure(error, maxRetries);

    if (exhausted) {
      notifyExhausted(inbox, exception);
    }
  }

  private void notifyExhausted(InboxEvent inbox, Exception exception) {
    try {
      EventHandler handler =
          handlerRegistry.get(inbox.getId().getConsumerId(), inbox.getEventType());
      if (handler instanceof InboxExhaustionHandler exhaustionHandler) {
        exhaustionHandler.onExhausted(inbox.toEventMessage(), exception);
      }
    } catch (Exception e) {
      // onExhausted 콜백이 실패해도 위의 recordFailure() 결과(FAILED 전환)는 그대로 유지
      log.error(
          "onExhausted 콜백 처리 중 오류. consumerId={}, eventType={}",
          inbox.getId().getConsumerId(),
          inbox.getEventType(),
          e);
    }
  }
}
