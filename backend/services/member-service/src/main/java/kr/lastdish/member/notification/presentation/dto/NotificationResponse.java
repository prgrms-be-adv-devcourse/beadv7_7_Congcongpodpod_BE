package kr.lastdish.member.notification.presentation.dto;

import kr.lastdish.member.notification.domain.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long id,
    String type,
    String title,
    String body,
    String data,
    String linkTarget,
    Long linkId,
    boolean readYn,
    LocalDateTime createdAt) {

  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getType(),
        notification.getTitle(),
        notification.getBody(),
        notification.getData(),
        notification.getLinkTarget(),
        notification.getLinkId(),
        notification.isReadYn(),
        notification.getCreatedAt());
  }
}
