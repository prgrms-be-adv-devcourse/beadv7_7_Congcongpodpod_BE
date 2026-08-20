package kr.lastdish.member.notification.infrastructure.sse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import kr.lastdish.member.notification.application.sse.SseConnectionService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseEmitterRegistry implements SseConnectionService {

  private final ConcurrentMap<Long, CopyOnWriteArrayList<SseEmitter>> emitters =
      new ConcurrentHashMap<>();

  @Override
  public void register(Long memberId, SseEmitter emitter) {
    emitters.computeIfAbsent(memberId, key -> new CopyOnWriteArrayList<>()).add(emitter);
    emitter.onCompletion(() -> remove(memberId, emitter));
    emitter.onTimeout(() -> remove(memberId, emitter));
    emitter.onError(error -> remove(memberId, emitter));
  }

  public void send(Long memberId, Object data) {
    List<SseEmitter> memberEmitters = emitters.get(memberId);
    if (memberEmitters == null) {
      return;
    }
    for (SseEmitter emitter : memberEmitters) {
      try {
        emitter.send(SseEmitter.event().name("notification").data(data));
      } catch (IOException | IllegalStateException exception) {
        remove(memberId, emitter);
      }
    }
  }

  private void remove(Long memberId, SseEmitter emitter) {
    emitters.computeIfPresent(
        memberId,
        (key, list) -> {
          list.remove(emitter);
          return list.isEmpty() ? null : list;
        });
  }
}
