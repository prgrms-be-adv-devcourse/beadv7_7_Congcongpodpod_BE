package kr.lastdish.member.notification.presentation;

import kr.lastdish.member.notification.application.sse.SseConnectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class NotificationSseController {

  private final SseConnectionService sseConnectionService;

  @GetMapping(value = "/api/v1/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@RequestHeader("X-Authenticated-Member-Id") Long memberId) {
    SseEmitter emitter = new SseEmitter(0L);
    sseConnectionService.register(memberId, emitter);
    return emitter;
  }
}