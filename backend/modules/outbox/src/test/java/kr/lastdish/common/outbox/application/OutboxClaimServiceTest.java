package kr.lastdish.common.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import kr.lastdish.common.outbox.domain.OutboxClaimRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxClaimServiceTest {

  @Mock private OutboxClaimRepository claimRepository;

  private OutboxClaimService claimService;

  @BeforeEach
  void setUp() {
    /*
     * 테스트에서는 batchSize 100개와 잠금 만료 60초를 사용합니다.
     */
    claimService = new OutboxClaimService(claimRepository, 100, 60);
  }

  @Test
  void claims_events_with_batch_size_and_lock_timeout() {
    // given
    List<UUID> claimedEventIds = List.of(UUID.randomUUID(), UUID.randomUUID());

    ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);

    ArgumentCaptor<Instant> expiredCaptor = ArgumentCaptor.forClass(Instant.class);

    when(claimRepository.claim(
            org.mockito.ArgumentMatchers.eq(100), nowCaptor.capture(), expiredCaptor.capture()))
        .thenReturn(claimedEventIds);

    // when
    List<UUID> result = claimService.claim();

    // then
    assertThat(result).isEqualTo(claimedEventIds);

    verify(claimRepository)
        .claim(
            org.mockito.ArgumentMatchers.eq(100),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any());

    /*
     * 만료 기준 시각이 현재 시각보다 정확히 60초 이전인지 검증합니다.
     */
    Duration timeout = Duration.between(expiredCaptor.getValue(), nowCaptor.getValue());

    assertThat(timeout).isEqualTo(Duration.ofSeconds(60));
  }
}
