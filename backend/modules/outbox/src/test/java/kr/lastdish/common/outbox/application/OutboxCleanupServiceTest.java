package kr.lastdish.common.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import kr.lastdish.common.outbox.domain.OutboxCleanupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxCleanupServiceTest {

  @Mock private OutboxCleanupRepository repository;

  @Test
  void cleanup_deletes_published_events_by_batch_size() {
    OutboxCleanupService service = new OutboxCleanupService(repository, 1000);
    given(repository.deletePublishedBatch(1000)).willReturn(1000);

    int deletedCount = service.cleanup();

    assertThat(deletedCount).isEqualTo(1000);
    then(repository).should().deletePublishedBatch(1000);
  }
}
