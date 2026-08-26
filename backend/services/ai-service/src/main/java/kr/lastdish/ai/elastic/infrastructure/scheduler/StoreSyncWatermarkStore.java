package kr.lastdish.ai.elastic.infrastructure.scheduler;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoreSyncWatermarkStore {

  private static final String KEY = "store-sync:last-synced-at";

  private final StringRedisTemplate redisTemplate;

  public Instant getLastSyncedAt() {
    String value = redisTemplate.opsForValue().get(KEY);
    if (value == null) {
      // 최초 실행: 전체 백필 방지, 폴링 주기만큼만 과거로 시작
      return Instant.now().minusSeconds(60);
    }
    // 정합성 최우선: 장애가 아무리 길어도 watermark를 강제로 앞당기지 않는다.
    // syncUpdatedStores가 limit 없이 구간 전체를 처리, 4xx/5xx는 예외를 던져 watermark를
    // 갱신하지 않으므로, 복구되는 순간 지연된 구간 전체를 반드시 그대로 따라잡는다.
    return Instant.parse(value);
  }

  public void updateLastSyncedAt(Instant instant) {
    redisTemplate.opsForValue().set(KEY, instant.toString());
  }
}
