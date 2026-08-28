package kr.lastdish.ai.elastic.infrastructure.scheduler;

import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StoreSyncWatermarkStore {

  private static final String KEY = "store-sync:last-synced-at";

  private final StringRedisTemplate redisTemplate;

  /**
   * 저장된 watermark를 그대로 반환, 없으면(최초 실행/삭제됨)
   */
  public Optional<Instant> getStoredWatermark() {
    String value = redisTemplate.opsForValue().get(KEY);
    return value == null ? Optional.empty() : Optional.of(Instant.parse(value));
  }

  public void updateLastSyncedAt(Instant instant) {
    redisTemplate.opsForValue().set(KEY, instant.toString());
  }
}
