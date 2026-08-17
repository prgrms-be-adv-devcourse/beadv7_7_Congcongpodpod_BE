package kr.lastdish.core.point.application;

import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.core.level.application.LevelService;
import kr.lastdish.core.point.application.dto.PointBalanceResponse;
import kr.lastdish.core.point.application.dto.PointTransactionResult;
import kr.lastdish.core.point.domain.Point;
import kr.lastdish.core.point.domain.PointHistory;
import kr.lastdish.core.point.domain.PointHistoryRepository;
import kr.lastdish.core.point.domain.PointNotFoundException;
import kr.lastdish.core.point.domain.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final LevelService levelService;

    @Transactional
    public Point getOrCreatePoint(Long memberId) {
        return pointRepository
                .findByMemberId(memberId)
                .orElseGet(() -> pointRepository.save(Point.createDefault(memberId)));
    }

    @Transactional
    public PointBalanceResponse getPointBalance(Long memberId) {
        return PointBalanceResponse.from(getOrCreatePoint(memberId));
    }

    // 포인트 적립 (Level 적립률 * 최종 주문 금액)
    @Transactional
    public PointTransactionResult earn(Long memberId, Long orderId, BigDecimal finalOrderAmount) {
        BigDecimal earningRate = levelService.getPointEarningRate(memberId);
        BigDecimal earnedAmount = finalOrderAmount.multiply(earningRate);

        Point point = pointRepository.findWithLockByMemberId(memberId)
                .orElseGet(() -> pointRepository.save(Point.createDefault(memberId)));

        point.earn(earnedAmount);

        PointHistory history = pointHistoryRepository.save(
                PointHistory.recordEarn(memberId, orderId, earnedAmount, point.getBalance()));

        return PointTransactionResult.from(history);
    }

    // 포인트 사용 (전체 잔액 차감 + FIFO 개별 적립건 소진)
    @Transactional
    public PointTransactionResult use(Long memberId, Long orderId, BigDecimal amountToUse) {
        Point point =
                pointRepository
                        .findWithLockByMemberId(memberId)
                        .orElseThrow(() -> new PointNotFoundException(memberId));

        point.use(amountToUse); // 잔액 부족 시 예외 발생

        consumeEarnHistories(memberId, amountToUse);

        PointHistory history = pointHistoryRepository.save(
                PointHistory.recordUse(memberId, orderId, amountToUse, point.getBalance()));

        return PointTransactionResult.from(history);
    }

    // FIFO 소진 : remainingAmount 남은 EARN건을 오래된 순으로 소진
    private void consumeEarnHistories(Long memberId, BigDecimal amountToConsume) {
        List<PointHistory> usableHistories = pointHistoryRepository.findUsableEarnHistories(memberId);
        BigDecimal remainingToConsume = amountToConsume;

        for (PointHistory history : usableHistories) {
            if (remainingToConsume.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal consumedAmount = history.getRemainingAmount().min(remainingToConsume);
            history.consume(consumedAmount);
            remainingToConsume = remainingToConsume.subtract(consumedAmount);
        }

        // balance와 적립 내역 합계 일치하는지 확인
        if (remainingToConsume.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    "포인트 잔액과 적립 내역 합계가 일치하지 않습니다. memberId=" + memberId);
        }
    }
}