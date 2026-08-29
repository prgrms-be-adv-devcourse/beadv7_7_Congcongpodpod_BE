package kr.lastdish.common.inbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.Duration;
import java.time.Instant;
import kr.lastdish.common.inbox.domain.InboxCleanupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboxCleanupServiceTest {

  @Mock private InboxCleanupRepository repository;

  @Test
  void cleanup_deletes_completed_events_after_retention_period() {
    InboxCleanupService service = new InboxCleanupService(repository, 3, 1000);
    given(repository.deleteCompletedBatch(org.mockito.ArgumentMatchers.any(), eq(1000)))
        .willReturn(1000);
    Instant beforeCleanup = Instant.now();

    int deletedCount = service.cleanup();

    Instant afterCleanup = Instant.now();
    ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
    assertThat(deletedCount).isEqualTo(1000);
    then(repository).should().deleteCompletedBatch(cutoffCaptor.capture(), eq(1000));
    assertThat(cutoffCaptor.getValue())
        .isBetween(beforeCleanup.minus(Duration.ofDays(3)), afterCleanup.minus(Duration.ofDays(3)));
  }
}
