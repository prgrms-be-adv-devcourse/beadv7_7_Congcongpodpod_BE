package kr.lastdish.common.outbox.application;

import kr.lastdish.common.event.DomainEvent;
import kr.lastdish.common.outbox.domain.OutboxEvent;
import kr.lastdish.common.outbox.domain.OutboxEventRepository;
import kr.lastdish.common.outbox.domain.OutboxEventSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 도메인 이벤트를 Outbox 테이블에 기록합니다.
 *
 * <p>이 클래스는 별도의 트랜잭션을 시작하지 않습니다. DishService 또는 OrderService에서 시작한 기존 트랜잭션에 참여해야 도메인 변경과 Outbox 저장의
 * 원자성을 보장할 수 있습니다.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventWriter {

  private final OutboxEventRepository repository;
  private final OutboxEventSerializer serializer;

  /**
   * 도메인 이벤트를 JSON으로 직렬화하여 PENDING Outbox 이벤트로 저장합니다.
   *
   * @param event 저장할 도메인 이벤트
   */
  public void append(DomainEvent<?> event) {
    // 공통 메타데이터를 제외한 처리할 payload만 JSON 문자열로 변환합니다.
    String payload = serializer.serialize(event.payload());

    // 이벤트 메타데이터와 JSON payload로 PENDING Outbox를 생성합니다.
    OutboxEvent outbox = OutboxEvent.create(event, payload);

    // 호출한 서비스의 트랜잭션에 참여하여 저장합니다.
    repository.save(outbox);
  }
}
