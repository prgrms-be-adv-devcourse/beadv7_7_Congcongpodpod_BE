package kr.lastdish.core.point.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.level.application.LevelService;
import kr.lastdish.core.level.application.dto.LevelPurchaseResult;
import kr.lastdish.core.level.domain.DishLevel;
import kr.lastdish.core.point.application.dto.PointTransactionResult;
import kr.lastdish.core.point.application.event.MemberRewardEventWriter;
import kr.lastdish.core.point.domain.PointType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointFacadeTest {

  @Mock private PointService pointService;
  @Mock private LevelService levelService;
  @Mock private MemberRewardEventWriter memberRewardEventWriter;

  @InjectMocks private PointFacade pointFacade;

  @Test
  void earnAndEvaluateLevel_이미_처리된_주문이면_전체_처리를_스킵한다() {
    Long memberId = 1L;
    Long orderId = 100L;
    given(pointService.hasAlreadyEarned(orderId)).willReturn(true);

    pointFacade.earnAndEvaluateLevel(
        memberId, orderId, new BigDecimal("10000"), new BigDecimal("1000"));

    verify(pointService, never()).earn(any(), any(), any());
    verify(levelService, never()).recordPurchase(any(), any(), any());
    verify(memberRewardEventWriter, never()).append(any(), any(), anyBoolean(), any());
  }

  @Test
  void earnAndEvaluateLevel_정상_처리시_적립_레벨반영_알림발행이_모두_호출된다() {
    Long memberId = 1L;
    Long orderId = 100L;
    BigDecimal finalOrderAmount = new BigDecimal("10000");
    BigDecimal savedAmount = new BigDecimal("1000");

    given(pointService.hasAlreadyEarned(orderId)).willReturn(false);

    PointTransactionResult earnResult =
        new PointTransactionResult(
            10L,
            PointType.EARN,
            new BigDecimal("500"),
            new BigDecimal("1500"),
            LocalDateTime.now());
    given(pointService.earn(memberId, orderId, finalOrderAmount)).willReturn(earnResult);

    LevelPurchaseResult levelResult = new LevelPurchaseResult(false, DishLevel.LEVEL_1);
    given(levelService.recordPurchase(memberId, orderId, savedAmount)).willReturn(levelResult);

    pointFacade.earnAndEvaluateLevel(memberId, orderId, finalOrderAmount, savedAmount);

    verify(pointService, times(1)).earn(memberId, orderId, finalOrderAmount);
    verify(levelService, times(1)).recordPurchase(memberId, orderId, savedAmount);
    verify(memberRewardEventWriter, times(1))
        .append(memberId, earnResult.amount(), false, DishLevel.LEVEL_1);
  }

  @Test
  void earnAndEvaluateLevel_승급했으면_알림에_승급_결과가_반영된다() {
    Long memberId = 1L;
    Long orderId = 100L;
    BigDecimal finalOrderAmount = new BigDecimal("10000");
    BigDecimal savedAmount = new BigDecimal("1000");

    given(pointService.hasAlreadyEarned(orderId)).willReturn(false);

    PointTransactionResult earnResult =
        new PointTransactionResult(
            10L,
            PointType.EARN,
            new BigDecimal("500"),
            new BigDecimal("1500"),
            LocalDateTime.now());
    given(pointService.earn(memberId, orderId, finalOrderAmount)).willReturn(earnResult);

    LevelPurchaseResult levelResult = new LevelPurchaseResult(true, DishLevel.LEVEL_2);
    given(levelService.recordPurchase(memberId, orderId, savedAmount)).willReturn(levelResult);

    pointFacade.earnAndEvaluateLevel(memberId, orderId, finalOrderAmount, savedAmount);

    verify(memberRewardEventWriter, times(1))
        .append(memberId, earnResult.amount(), true, DishLevel.LEVEL_2);
  }

  @Test
  void earnAndEvaluateLevel_포인트_적립에서_예외가_발생하면_레벨반영과_알림발행은_호출되지_않는다() {
    Long memberId = 1L;
    Long orderId = 100L;
    BigDecimal finalOrderAmount = new BigDecimal("10000");
    BigDecimal savedAmount = new BigDecimal("1000");

    given(pointService.hasAlreadyEarned(orderId)).willReturn(false);
    given(pointService.earn(memberId, orderId, finalOrderAmount))
        .willThrow(new IllegalStateException("강제 실패"));

    Assertions.assertThrows(
        IllegalStateException.class,
        () -> pointFacade.earnAndEvaluateLevel(memberId, orderId, finalOrderAmount, savedAmount));

    verify(levelService, never()).recordPurchase(any(), any(), any());
    verify(memberRewardEventWriter, never()).append(any(), any(), anyBoolean(), any());
  }
}
