package kr.lastdish.core.point.infrastructure;

import java.util.List;
import kr.lastdish.core.point.domain.PointHistory;
import kr.lastdish.core.point.domain.PointHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointHistoryRepositoryImpl implements PointHistoryRepository {

  private final PointHistoryJpaRepository pointHistoryJpaRepository;

  @Override
  public PointHistory save(PointHistory pointHistory) {
    return pointHistoryJpaRepository.save(pointHistory);
  }

  @Override
  public List<PointHistory> findUsableEarnHistories(Long memberId) {
    return pointHistoryJpaRepository.findUsableEarnHistories(memberId);
  }

  @Override
  public List<PointHistory> findAll() {
    return pointHistoryJpaRepository.findAll();
  }

  @Override
  public void deleteAll() {
    pointHistoryJpaRepository.deleteAll();
  }
}
