package kr.lastdish.core.point.infrastructure;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.lastdish.core.point.domain.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PointJpaRepository extends JpaRepository<Point, Long> {

    Optional<Point> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Point> findWithLockByMemberId(Long memberId);
}