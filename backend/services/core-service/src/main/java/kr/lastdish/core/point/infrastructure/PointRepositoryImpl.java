package kr.lastdish.core.point.infrastructure;

import java.util.List;
import java.util.Optional;
import kr.lastdish.core.point.domain.Point;
import kr.lastdish.core.point.domain.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PointRepositoryImpl implements PointRepository {

    private final PointJpaRepository pointJpaRepository;

    @Override
    public Point save(Point point) {
        return pointJpaRepository.save(point);
    }

    @Override
    public Optional<Point> findByMemberId(Long memberId) {
        return pointJpaRepository.findByMemberId(memberId);
    }

    @Override
    public Optional<Point> findWithLockByMemberId(Long memberId) {
        return pointJpaRepository.findWithLockByMemberId(memberId);
    }

    @Override
    public List<Point> findAll() {
        return pointJpaRepository.findAll();
    }

    @Override
    public void deleteAll() {
        pointJpaRepository.deleteAll();
    }
}