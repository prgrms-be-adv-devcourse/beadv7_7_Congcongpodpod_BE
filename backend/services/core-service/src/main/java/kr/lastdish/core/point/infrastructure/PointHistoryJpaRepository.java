package kr.lastdish.core.point.infrastructure;

import java.math.BigDecimal;
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

  @Query(
      """
    SELECT DISTINCT h.memberId
    FROM PointHistory h
    WHERE h.expiresAt <= CURRENT_TIMESTAMP
      AND h.remainingAmount > 0
      AND h.type = kr.lastdish.core.point.domain.PointType.EARN
    """)
  List<Long> findMembersWithExpiringPoints();

  @Query(
      """
    SELECT h
    FROM PointHistory h
    WHERE h.memberId = :memberId
      AND h.expiresAt <= CURRENT_TIMESTAMP
      AND h.remainingAmount > 0
      AND h.type = kr.lastdish.core.point.domain.PointType.EARN
    """)
  List<PointHistory> findExpiringHistoriesByMember(@Param("memberId") Long memberId);

  @Query(
      """
      SELECT COALESCE(SUM(h.remainingAmount), 0)
      FROM PointHistory h
      WHERE h.memberId = :memberId
        AND h.type = kr.lastdish.core.point.domain.PointType.EARN
        AND h.remainingAmount > 0
        AND h.expiresAt <= CURRENT_TIMESTAMP
      """)
  BigDecimal sumExpiringAmountByMember(@Param("memberId") Long memberId);
}
