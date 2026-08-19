package kr.lastdish.core.point.infrastructure;

import java.util.List;
import kr.lastdish.core.point.domain.PointHistory;
import kr.lastdish.core.point.domain.PointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointHistoryJpaRepository extends JpaRepository<PointHistory, Long> {

  @Query(
      """

                  SELECT h FROM PointHistory h
      WHERE h.memberId = :memberId
        AND h.type = kr.lastdish.core.point.domain.PointType.EARN
        AND h.remainingAmount > 0
        AND h.expiresAt > CURRENT_TIMESTAMP
      ORDER BY h.createdAt ASC
      """)
  List<PointHistory> findUsableEarnHistories(@Param("memberId") Long memberId);

  boolean existsByOrderIdAndType(Long orderId, PointType type);
}
