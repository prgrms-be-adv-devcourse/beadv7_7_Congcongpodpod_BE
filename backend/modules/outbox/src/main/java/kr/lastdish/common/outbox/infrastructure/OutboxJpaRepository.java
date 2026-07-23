package kr.lastdish.common.outbox.infrastructure;

import java.util.UUID;
import kr.lastdish.common.outbox.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** OutboxEvent의 Spring Data JPA 저장소입니다. */
public interface OutboxJpaRepository extends JpaRepository<OutboxEvent, UUID> {}
