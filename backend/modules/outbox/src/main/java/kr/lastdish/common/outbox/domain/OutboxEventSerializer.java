package kr.lastdish.common.outbox.domain;

/** Outbox 이벤트 payload를 저장 가능한 문자열로 변환하는 계약입니다. */
public interface OutboxEventSerializer {

  /**
   * 이벤트 payload를 직렬화합니다.
   *
   * @param payload 직렬화할 이벤트 payload
   * @return 직렬화된 payload
   */
  String serialize(Object payload);
}
