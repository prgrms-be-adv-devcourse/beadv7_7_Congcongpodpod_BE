package kr.lastdish.ai.elastic.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class StoreSyncWatermarkStoreTest {

  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  @InjectMocks private StoreSyncWatermarkStore watermarkStore;

  @Test
  @DisplayName("Redis에 저장된 값이 없으면 Optional.empty()를 반환한다")
  void getStoredWatermark_noStoredValue_returnsEmptyOptional() {
    // given
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("store-sync:last-synced-at")).willReturn(null);

    // when
    Optional<Instant> result = watermarkStore.getStoredWatermark();

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("Redis에 저장된 값이 있으면 해당 시각이 포함된 Optional을 반환한다")
  void getStoredWatermark_recentStoredValue_returnsStoredValue() {
    // given
    Instant stored = Instant.now().minus(Duration.ofMinutes(5));
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("store-sync:last-synced-at")).willReturn(stored.toString());

    // when
    Optional<Instant> result = watermarkStore.getStoredWatermark();

    // then
    assertThat(result).isPresent().contains(stored);
  }

  @Test
  @DisplayName("Redis에 저장된 값이 아무리 오래돼도 클램핑 없이 그대로 반환한다")
  void getStoredWatermark_veryOldStoredValue_returnsAsIsWithoutClamping() {
    // given
    Instant stored = Instant.now().minus(Duration.ofDays(1));
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("store-sync:last-synced-at")).willReturn(stored.toString());

    // when
    Optional<Instant> result = watermarkStore.getStoredWatermark();

    // then
    assertThat(result).isPresent().contains(stored);
  }

  @Test
  @DisplayName("updateLastSyncedAt 호출 시 Redis에 ISO-8601 문자열로 저장된다")
  void updateLastSyncedAt_storesInstantAsString() {
    // given
    Instant now = Instant.parse("2026-08-24T10:00:00Z");
    given(redisTemplate.opsForValue()).willReturn(valueOperations);

    // when
    watermarkStore.updateLastSyncedAt(now);

    // then
    verify(valueOperations).set("store-sync:last-synced-at", "2026-08-24T10:00:00Z");
  }
}
