package kr.lastdish.core.level.infrastructure;

import io.lettuce.core.dynamic.annotation.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.lastdish.core.level.domain.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LevelJpaRepository extends JpaRepository<Level, Long> {

  Optional<Level> findByMemberId(Long memberId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Level> findWithLockByMemberId(Long memberId);

  @Modifying(flushAutomatically = true)
  @Query(
      value =
          """
          INSERT INTO levels (
              member_id,
              dish_level,
              purchase_count,
              discount_amount,
              updated_at
          )
          VALUES (:memberId, 'LEVEL_1', 0, 0, NOW())
          ON CONFLICT (member_id) DO NOTHING
          """,
      nativeQuery = true)
  void createDefaultIfAbsent(@Param("memberId") Long memberId);
}
