package kr.lastdish.core.level.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.lastdish.core.level.domain.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface LevelJpaRepository extends JpaRepository<Level, Long> {

  Optional<Level> findByMemberId(Long memberId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Level> findWithLockByMemberId(Long memberId);
}
