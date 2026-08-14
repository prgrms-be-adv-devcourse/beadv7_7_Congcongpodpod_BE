package kr.lastdish.core.level.infrastructure;

import kr.lastdish.core.level.domain.LevelHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelHistoryJpaRepository extends JpaRepository<LevelHistory, Long> {}
