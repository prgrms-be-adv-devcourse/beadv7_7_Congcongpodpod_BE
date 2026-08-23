package kr.lastdish.core.point.domain;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PointHistoryRepository {
  PointHistory save(PointHistory pointHistory);

  List<PointHistory> findUsableEarnHistories(Long memberId); // 만료 안 됐고 remainingAmount 남은 EARN건

  boolean existsByOrderIdAndType(Long orderId, PointType type);

  List<Long> findMembersWithExpiringPoints();

  List<PointHistory> findExpiringHistoriesByMember(Long memberId);

  BigDecimal sumExpiringAmountByMember(Long memberId);

  Page<PointHistory> findByMemberId(Long memberId, Pageable pageable);

  List<PointHistory> findAll();

  void deleteAll();
}
