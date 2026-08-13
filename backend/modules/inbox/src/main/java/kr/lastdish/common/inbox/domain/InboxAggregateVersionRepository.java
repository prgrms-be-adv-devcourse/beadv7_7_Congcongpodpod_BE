package kr.lastdish.common.inbox.domain;

import java.time.Instant;

public interface InboxAggregateVersionRepository {

  InboxAggregateVersion getOrCreateAndLock(InboxAggregateVersionId id, Instant now);
}
