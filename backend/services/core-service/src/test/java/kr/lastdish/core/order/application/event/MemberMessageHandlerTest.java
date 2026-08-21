package kr.lastdish.core.order.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import kr.lastdish.common.inbox.domain.InboxProcessingPolicy;
import kr.lastdish.core.order.application.MemberSnapshotSynchronizer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class MemberMessageHandlerTest {

  @Test
  void 세_이벤트가_같은_consumerId와_최신값_우선_정책을_사용한다() {
    ObjectMapper objectMapper = new ObjectMapper();
    MemberSnapshotSynchronizer synchronizer = mock(MemberSnapshotSynchronizer.class);
    MemberCreatedMessageHandler created =
        new MemberCreatedMessageHandler(objectMapper, synchronizer);
    MemberUpdatedMessageHandler updated =
        new MemberUpdatedMessageHandler(objectMapper, synchronizer);
    MemberDeletedMessageHandler deleted =
        new MemberDeletedMessageHandler(objectMapper, synchronizer);

    assertThat(created.consumerId())
        .isEqualTo(updated.consumerId())
        .isEqualTo(deleted.consumerId())
        .isEqualTo("core-order-member-snapshot");
    assertThat(created.processingPolicy()).isEqualTo(InboxProcessingPolicy.IDEMPOTENT_LATEST_WINS);
    assertThat(updated.processingPolicy()).isEqualTo(InboxProcessingPolicy.IDEMPOTENT_LATEST_WINS);
    assertThat(deleted.processingPolicy()).isEqualTo(InboxProcessingPolicy.IDEMPOTENT_LATEST_WINS);
  }
}
