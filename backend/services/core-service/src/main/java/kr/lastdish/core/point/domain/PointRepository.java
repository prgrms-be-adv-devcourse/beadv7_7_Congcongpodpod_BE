package kr.lastdish.core.point.domain;

import java.util.List;
import java.util.Optional;

public interface PointRepository {
  Point save(Point point);

  Optional<Point> findByMemberId(Long memberId);

  Optional<Point> findWithLockByMemberId(Long memberId);

  void createDefaultIfAbsent(Long memberId);

  // 통합 테스트 DB 초기화 전용 (Deposit과 동일)
  List<Point> findAll();

  void deleteAll();
}
