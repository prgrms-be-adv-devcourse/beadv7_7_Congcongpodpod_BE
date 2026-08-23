package kr.lastdish.ai.infrastructure.scheduler;

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
        return Instant.parse(value);
    }

    public void updateLastSyncedAt(Instant instant) {
        redisTemplate.opsForValue().set(KEY, instant.toString());
    }
}