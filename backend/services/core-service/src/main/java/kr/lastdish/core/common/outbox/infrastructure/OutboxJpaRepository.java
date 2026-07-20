package kr.lastdish.core.common.outbox.infrastructure;

import kr.lastdish.core.common.outbox.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * OutboxEvent의 Spring Data JPA 저장소입니다.
 */
public interface OutboxJpaRepository
    extends JpaRepository<OutboxEvent, UUID> {
}