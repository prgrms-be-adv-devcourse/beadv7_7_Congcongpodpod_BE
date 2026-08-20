package kr.lastdish.member.notification.presentation;

import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.member.notification.application.NotificationService;
import kr.lastdish.member.notification.presentation.dto.NotificationResponse;
import kr.lastdish.member.notification.presentation.dto.PageResponse;
import kr.lastdish.member.notification.presentation.dto.UnreadCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ApiResponse<PageResponse<NotificationResponse>> getNotifications(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ApiResponse.ok(notificationService.getNotifications(memberId, pageable));
  }

  @GetMapping("/unread-count")
  public ApiResponse<UnreadCountResponse> getUnreadCount(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    return ApiResponse.ok(new UnreadCountResponse(notificationService.getUnreadCount(memberId)));
  }

  @PatchMapping("/{notificationId}/read")
  public ApiResponse<Void> markAsRead(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @PathVariable Long notificationId) {
    notificationService.markAsRead(memberId, notificationId);
    return ApiResponse.ok();
  }

  @PatchMapping("/read-all")
  public ApiResponse<Void> markAllAsRead(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    notificationService.markAllAsRead(memberId);
    return ApiResponse.ok();
  }
}
