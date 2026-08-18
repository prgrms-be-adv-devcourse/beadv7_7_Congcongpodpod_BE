package kr.lastdish.ai.infrastructure.persistence;

import kr.lastdish.ai.infrastructure.persistence.entity.ClassificationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassificationLogJpaRepository
    extends JpaRepository<ClassificationLogEntity, Long> {}
