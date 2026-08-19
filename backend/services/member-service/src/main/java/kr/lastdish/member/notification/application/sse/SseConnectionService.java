package kr.lastdish.member.notification.application.sse;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseConnectionService {

  void register(Long memberId, SseEmitter emitter);
}