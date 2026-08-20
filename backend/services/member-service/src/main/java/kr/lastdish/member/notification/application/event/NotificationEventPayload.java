package kr.lastdish.member.notification.application.event;

public record NotificationEventPayload(
    Long memberId,
    String type,
    String title,
    String body,
    String data,
    String linkTarget,
    Long linkId) {}
