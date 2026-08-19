package kr.lastdish.core.point.application;

import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.core.point.domain.Point;
import kr.lastdish.core.point.domain.PointHistory;
import kr.lastdish.core.point.domain.PointHistoryRepository;
import kr.lastdish.core.point.domain.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointExpirationService {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    @Transactional
    public void expireMemberPoints(Long memberId) {
        Point point = pointRepository.findWithLockByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("포인트 정보가 없습니다. memberId=" + memberId));
        expireDueHistories(point);
    }

    // 이미 락을 획득한 Point를 받아 만료 대상만 소멸 처리
    public void expireDueHistories(Point point) {
        Long memberId = point.getMemberId();
        List<PointHistory> expiringHistories =
                pointHistoryRepository.findExpiringHistoriesByMember(memberId);

        // 개별 EARN 내역 소진
        BigDecimal totalExpiredAmount = BigDecimal.ZERO;
        for (PointHistory history : expiringHistories) {
            BigDecimal expiredAmount = history.getRemainingAmount();
            history.consume(expiredAmount);
            totalExpiredAmount = totalExpiredAmount.add(expiredAmount);
        }

        if (totalExpiredAmount.compareTo(BigDecimal.ZERO) > 0) {
            point.expire(totalExpiredAmount);
            pointHistoryRepository.save(
                    PointHistory.recordExpire(memberId, totalExpiredAmount, point.getBalance()));
        }
    }
}