package kr.lastdish.common.inbox.domain;

public enum InboxProcessingPolicy {
  /** eventId 중복만 방지하며 도착 순서는 고려하지 않는다. */
  IDEMPOTENT,

  /** 중복을 방지하고 마지막 적용 버전 이하의 과거 상태를 무시한다. */
  IDEMPOTENT_LATEST_WINS
}
