package kr.lastdish.core.level.infrastructure;

import java.util.Optional;
import kr.lastdish.core.level.domain.Level;
import kr.lastdish.core.level.domain.LevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LevelRepositoryImpl implements LevelRepository {

  private final LevelJpaRepository levelJpaRepository;

  @Override
  public Level save(Level level) {
    return levelJpaRepository.save(level);
  }

  @Override
  public Optional<Level> findByMemberId(Long memberId) {
    return levelJpaRepository.findByMemberId(memberId);
  }

  @Override
  public Optional<Level> findWithLockByMemberId(Long memberId) {
    return levelJpaRepository.findWithLockByMemberId(memberId);
  }
}
