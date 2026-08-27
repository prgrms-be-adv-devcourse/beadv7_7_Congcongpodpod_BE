package kr.lastdish.core.order.domain.event;

public record OrderNotificationPayload(
    Long memberId,
    String type,
    String title,
    String body,
    String data,
    String linkTarget,
    Long linkId) {}
