package kr.lastdish.common.inbox.domain;

import java.time.Instant;
import java.util.List;

public interface InboxClaimRepository {

  List<InboxEventId> claim(int batchSize, Instant now, Instant lockExpiredBefore);
}
