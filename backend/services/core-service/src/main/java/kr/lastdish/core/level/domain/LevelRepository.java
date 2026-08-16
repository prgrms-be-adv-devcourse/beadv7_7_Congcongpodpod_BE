package kr.lastdish.core.level.domain;

import java.util.Optional;

public interface LevelRepository {
  Level save(Level level);

  Optional<Level> findByMemberId(Long memberId);

  Optional<Level> findWithLockByMemberId(Long memberId);
}
