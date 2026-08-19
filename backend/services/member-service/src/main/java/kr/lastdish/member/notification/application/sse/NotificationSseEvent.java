package kr.lastdish.member.notification.application.sse;

import kr.lastdish.member.notification.presentation.dto.NotificationResponse;

public record NotificationSseEvent(Long memberId, NotificationResponse notification) {}