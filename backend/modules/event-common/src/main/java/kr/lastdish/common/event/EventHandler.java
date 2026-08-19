package kr.lastdish.common.event;

/** 전달 경로(Reliable/Inbox, Direct)와 무관하게 이벤트를 처리하는 핸들러 계약입니다. */
public interface EventHandler {

  String consumerId();

  String eventType();

  void handle(EventMessage message);
}
