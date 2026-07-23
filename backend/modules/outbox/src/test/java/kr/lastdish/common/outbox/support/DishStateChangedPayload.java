package kr.lastdish.common.outbox.support;

/** Outbox 모듈 테스트에서 사용하는 이벤트 payload fixture입니다. */
public record DishStateChangedPayload(boolean available, Long stockQuantity) {}
