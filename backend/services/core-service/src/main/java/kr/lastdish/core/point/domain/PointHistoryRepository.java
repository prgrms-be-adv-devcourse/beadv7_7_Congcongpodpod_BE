package kr.lastdish.core.point.domain;

import java.util.List;

public interface PointHistoryRepository {
    PointHistory save(PointHistory pointHistory);
    List<PointHistory> findUsableEarnHistories(Long memberId); // 만료 안 됐고 remainingAmount 남은 EARN건
    List<PointHistory> findAll();
    void deleteAll();
}