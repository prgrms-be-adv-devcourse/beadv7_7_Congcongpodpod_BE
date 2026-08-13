package kr.lastdish.common.inbox.infrastructure;

import kr.lastdish.common.inbox.domain.InboxEvent;
import kr.lastdish.common.inbox.domain.InboxEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxJpaRepository extends JpaRepository<InboxEvent, InboxEventId> {}
