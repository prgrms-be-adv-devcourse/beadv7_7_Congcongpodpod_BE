package kr.lastdish.core.level.infrastructure;

import kr.lastdish.core.level.domain.LevelHistory;
import kr.lastdish.core.level.domain.LevelHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LevelHistoryRepositoryImpl implements LevelHistoryRepository {

  private final LevelHistoryJpaRepository levelHistoryJpaRepository;

  @Override
  public LevelHistory save(LevelHistory levelHistory) {
    return levelHistoryJpaRepository.save(levelHistory);
  }

  @Override
  public boolean existsByOrderId(Long orderId) {
    return levelHistoryJpaRepository.existsByOrderId(orderId);
  }
}
