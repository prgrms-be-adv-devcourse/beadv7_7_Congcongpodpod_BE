package kr.lastdish.core.point.infrastructure;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.lastdish.core.point.domain.Point;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointJpaRepository extends JpaRepository<Point, Long> {

    Optional<Point> findByMemberId(Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Point> findWithLockByMemberId(Long memberId);

    @Modifying(flushAutomatically = true)
    @Query(
            value = """
          INSERT INTO points (
              member_id,
              balance,
              updated_at
          )
          VALUES (:memberId, 0, NOW())
          ON CONFLICT (member_id) DO NOTHING
          """,
            nativeQuery = true)
    void createDefaultIfAbsent(@Param("memberId") Long memberId);
}