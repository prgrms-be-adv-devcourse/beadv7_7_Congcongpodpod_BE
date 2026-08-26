package kr.lastdish.common.inbox.domain;

import kr.lastdish.common.event.EventMessage;

/** Inbox 재시도가 모두 소진되어 FAILED로 전환될 때 호출되는 옵트인 콜백 계약입니다. */
public interface InboxExhaustionHandler {

  void onExhausted(EventMessage message, Throwable lastError);
}
