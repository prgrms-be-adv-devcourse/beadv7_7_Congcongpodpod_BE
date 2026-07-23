package kr.lastdish.common.outbox.application;

import java.util.UUID;
import kr.lastdish.common.outbox.domain.OutboxEvent;
import kr.lastdish.common.outbox.domain.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox 이벤트 발행 실패 정보를 저장합니다.
 *
 * <p>OutboxEventProcessor 트랜잭션은 발행 실패 시 롤백됩니다. 같은 트랜잭션에서 실패 횟수를 변경하면 그 기록도 함께 롤백되므로 별도의
 * REQUIRES_NEW 트랜잭션에서 실패 정보를 저장합니다.
 */
@Service
public class OutboxFailureRecorder {

  private final OutboxEventRepository repository;
  private final int maxRetries;

  /**
   * 최대 재시도 횟수를 설정에서 주입받습니다.
   *
   * <p>설정이 없으면 기본값으로 5회를 사용합니다.
   */
  public OutboxFailureRecorder(
      OutboxEventRepository repository, @Value("${outbox.max-retries:5}") int maxRetries) {
    this.repository = repository;
    this.maxRetries = maxRetries;
  }

  /**
   * 발행 실패 횟수와 오류 메시지를 저장합니다.
   *
   * <p>최대 재시도 횟수에 도달하지 않았으면 PENDING으로 되돌리고, 최대 횟수에 도달하면 FAILED로 변경합니다.
   *
   * @param eventId 처리에 실패한 Outbox 이벤트 식별자
   * @param exception 발행 중 발생한 예외
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(UUID eventId, Exception exception) {
    OutboxEvent outbox =
        repository
            .findById(eventId)
            .orElseThrow(() -> new IllegalArgumentException("Outbox 이벤트를 찾을 수 없습니다: " + eventId));

    /*
     * 예외 메시지가 null인 경우에도 운영자가 실패 유형을 확인할 수 있도록
     * 예외 클래스 이름을 대신 저장합니다.
     */
    String errorMessage =
        exception.getMessage() != null
            ? exception.getMessage()
            : exception.getClass().getSimpleName();

    outbox.recordFailure(errorMessage, maxRetries);
  }
}
