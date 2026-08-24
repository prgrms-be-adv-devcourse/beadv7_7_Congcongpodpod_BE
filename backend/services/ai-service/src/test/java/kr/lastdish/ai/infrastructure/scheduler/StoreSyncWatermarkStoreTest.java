package kr.lastdish.ai.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.Duration;
import java.time.Instant;
import kr.lastdish.ai.elastic.infrastructure.scheduler.StoreSyncWatermarkStore;
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
  @DisplayName("Redis에 저장된 값이 없으면 60초 전 시각을 반환한다")
  void getLastSyncedAt_noStoredValue_returnsSixtySecondsAgo() {
    // given
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("store-sync:last-synced-at")).willReturn(null);

    // when
    Instant result = watermarkStore.getLastSyncedAt();

    // then
    assertThat(result).isBetween(Instant.now().minusSeconds(65), Instant.now().minusSeconds(55));
  }

  @Test
  @DisplayName("Redis에 저장된 값이 있고 1시간 이내면 그 값을 그대로 반환한다")
  void getLastSyncedAt_recentStoredValue_returnsStoredValue() {
    // given
    Instant stored = Instant.now().minus(Duration.ofMinutes(5));
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("store-sync:last-synced-at")).willReturn(stored.toString());

    // when
    Instant result = watermarkStore.getLastSyncedAt();

    // then
    assertThat(result).isEqualTo(stored);
  }

  @Test
  @DisplayName("Redis에 저장된 값이 1시간보다 오래됐으면 최대 1시간 전으로 클램핑된다")
  void getLastSyncedAt_tooOldStoredValue_clampsToMaxLookback() {
    // given
    Instant stored = Instant.now().minus(Duration.ofDays(1));
    given(redisTemplate.opsForValue()).willReturn(valueOperations);
    given(valueOperations.get("store-sync:last-synced-at")).willReturn(stored.toString());

    // when
    Instant result = watermarkStore.getLastSyncedAt();

    // then: 정확히 1시간 전 근처(수 초 오차 허용)여야 하며, 저장된 값(1일 전)보다 훨씬 최근이어야 함
    assertThat(result).isAfter(stored);
    assertThat(result)
        .isBetween(
            Instant.now().minus(Duration.ofHours(1)).minusSeconds(5),
            Instant.now().minus(Duration.ofHours(1)).plusSeconds(5));
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
    org.mockito.Mockito.verify(valueOperations)
        .set("store-sync:last-synced-at", "2026-08-24T10:00:00Z");
  }
}
