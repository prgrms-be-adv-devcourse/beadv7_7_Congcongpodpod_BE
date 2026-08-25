package kr.lastdish.core.point.application;

import java.math.BigDecimal;
import kr.lastdish.core.level.application.LevelService;
import kr.lastdish.core.level.application.dto.LevelPurchaseResult;
import kr.lastdish.core.point.application.dto.PointTransactionResult;
import kr.lastdish.core.point.application.event.MemberRewardEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointFacade {

  private final PointService pointService;
  private final LevelService levelService;
  private final MemberRewardEventWriter memberRewardEventWriter;

  @Transactional
  public void earnAndEvaluateLevel(
      Long memberId, Long orderId, BigDecimal finalOrderAmount, BigDecimal savedAmount) {

    if (pointService.hasAlreadyEarned(orderId)) {
      log.info("이미 처리 완료된 주문입니다. 스킵합니다. orderId={}", orderId);
      return;
    }

    PointTransactionResult earnResult = pointService.earn(memberId, orderId, finalOrderAmount);
    LevelPurchaseResult levelResult = levelService.recordPurchase(memberId, orderId, savedAmount);

    memberRewardEventWriter.append(
        memberId, earnResult.amount(), levelResult.upgraded(), levelResult.currentLevel());
  }
}
