package kr.lastdish.core.point.domain;

import java.util.List;
import java.util.Optional;

public interface PointRepository {
    Point save(Point point);
    Optional<Point> findByMemberId(Long memberId);
    Optional<Point> findWithLockByMemberId(Long memberId);
    List<Point> findAll();
    void deleteAll();
}