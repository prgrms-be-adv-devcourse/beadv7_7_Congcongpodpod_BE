package kr.lastdish.member.notification.application.dto;

import java.util.UUID;

public record CreateNotificationCommand(
    Long memberId,
    String type,
    String title,
    String body,
    String data,
    String linkTarget,
    Long linkId,
    UUID eventId) {}
