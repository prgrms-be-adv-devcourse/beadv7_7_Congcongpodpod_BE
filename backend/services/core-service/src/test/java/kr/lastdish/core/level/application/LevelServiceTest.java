package kr.lastdish.core.level.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.level.application.dto.LevelPurchaseResult;
import kr.lastdish.core.level.application.dto.LevelResponse;
import kr.lastdish.core.level.domain.DishLevel;
import kr.lastdish.core.level.domain.Level;
import kr.lastdish.core.level.domain.LevelHistory;
import kr.lastdish.core.level.domain.LevelHistoryRepository;
import kr.lastdish.core.level.domain.LevelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LevelServiceTest {

  @Mock private LevelRepository levelRepository;
  @Mock private LevelHistoryRepository levelHistoryRepository;

  @InjectMocks private LevelService levelService;

  @Test
  void getOrDefaultLevel_기존_레벨이_없으면_기본값을_반환하고_저장하지_않는다() {
    Long memberId = 1L;
    given(levelRepository.findByMemberId(memberId)).willReturn(Optional.empty());

    Level result = levelService.getOrDefaultLevel(memberId);

    assertThat(result.getDishLevel()).isEqualTo(DishLevel.LEVEL_1);
    assertThat(result.getPurchaseCount()).isEqualTo(0);
    verify(levelRepository, never()).save(any(Level.class));
  }

  @Test
  void getOrDefaultLevel_기존_레벨이_있으면_그대로_반환한다() {
    Long memberId = 1L;
    Level existing = Level.createDefault(memberId);
    given(levelRepository.findByMemberId(memberId)).willReturn(Optional.of(existing));

    Level result = levelService.getOrDefaultLevel(memberId);

    assertThat(result).isEqualTo(existing);
    verify(levelRepository, never()).save(any(Level.class));
  }

  @Test
  void recordPurchase_이미_처리된_주문이면_기존_등급을_그대로_반환하고_아무것도_하지_않는다() {
    Long memberId = 1L;
    Long orderId = 100L;
    Level existing = Level.createDefault(memberId);
    given(levelHistoryRepository.existsByOrderId(orderId)).willReturn(true);
    given(levelRepository.findByMemberId(memberId)).willReturn(Optional.of(existing));

    LevelPurchaseResult result =
        levelService.recordPurchase(memberId, orderId, new BigDecimal("1000"));

    assertThat(result.upgraded()).isFalse();
    assertThat(result.currentLevel()).isEqualTo(DishLevel.LEVEL_1);
    verify(levelRepository, never()).createDefaultIfAbsent(any());
    verify(levelRepository, never()).findWithLockByMemberId(any());
    verify(levelHistoryRepository, never()).save(any(LevelHistory.class));
  }

  @Test
  void recordPurchase_할인금액이_null이면_예외가_발생한다() {
    Long memberId = 1L;
    Long orderId = 100L;

    assertThrows(
        BusinessException.class, () -> levelService.recordPurchase(memberId, orderId, null));

    verify(levelHistoryRepository, never()).existsByOrderId(any());
    verify(levelRepository, never()).createDefaultIfAbsent(any());
  }

  @Test
  void recordPurchase_할인금액이_음수이면_예외가_발생한다() {
    Long memberId = 1L;
    Long orderId = 100L;

    assertThrows(
        BusinessException.class,
        () -> levelService.recordPurchase(memberId, orderId, new BigDecimal("-1")));

    verify(levelRepository, never()).createDefaultIfAbsent(any());
  }

  @Test
  void recordPurchase_중복이_아니면_createDefaultIfAbsent를_먼저_호출한다() {
    Long memberId = 1L;
    Long orderId = 100L;
    Level level = Level.createDefault(memberId);
    given(levelHistoryRepository.existsByOrderId(orderId)).willReturn(false);
    given(levelRepository.findWithLockByMemberId(memberId)).willReturn(Optional.of(level));

    levelService.recordPurchase(memberId, orderId, new BigDecimal("1000"));

    verify(levelRepository, times(1)).createDefaultIfAbsent(memberId);
    verify(levelRepository, never()).save(any(Level.class));
  }

  @Test
  void recordPurchase_생성_보장_이후에도_조회에_실패하면_예외가_발생한다() {
    Long memberId = 1L;
    Long orderId = 100L;
    given(levelHistoryRepository.existsByOrderId(orderId)).willReturn(false);
    given(levelRepository.findWithLockByMemberId(memberId)).willReturn(Optional.empty());

    assertThrows(
        IllegalStateException.class,
        () -> levelService.recordPurchase(memberId, orderId, new BigDecimal("1000")));

    verify(levelRepository, times(1)).createDefaultIfAbsent(memberId);
  }

  @Test
  void recordPurchase_승급하면_LevelHistory가_저장되고_승급_결과를_반환한다() {
    Long memberId = 1L;
    Long orderId = 100L;
    Level level = Level.createDefault(memberId);
    for (int i = 0; i < 4; i++) {
      level.addPurchase(); // 구매횟수 4로 세팅 (이번 구매로 5 -> 승급)
    }
    given(levelHistoryRepository.existsByOrderId(orderId)).willReturn(false);
    given(levelRepository.findWithLockByMemberId(memberId)).willReturn(Optional.of(level));

    LevelPurchaseResult result =
        levelService.recordPurchase(memberId, orderId, new BigDecimal("1000"));

    assertThat(result.upgraded()).isTrue();
    assertThat(result.currentLevel()).isEqualTo(DishLevel.LEVEL_2);
    verify(levelHistoryRepository, times(1)).save(any(LevelHistory.class));
  }

  @Test
  void recordPurchase_승급하지_않아도_LevelHistory는_저장된다() {
    Long memberId = 1L;
    Long orderId = 100L;
    Level level = Level.createDefault(memberId); // 구매횟수 0
    given(levelHistoryRepository.existsByOrderId(orderId)).willReturn(false);
    given(levelRepository.findWithLockByMemberId(memberId)).willReturn(Optional.of(level));

    LevelPurchaseResult result =
        levelService.recordPurchase(memberId, orderId, new BigDecimal("1000"));

    assertThat(result.upgraded()).isFalse();

    ArgumentCaptor<LevelHistory> captor = ArgumentCaptor.forClass(LevelHistory.class);
    verify(levelHistoryRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().isUpgrade()).isFalse();
    assertThat(captor.getValue().getOrderId()).isEqualTo(orderId);
  }

  @Test
  void recordPurchase_호출하면_할인금액이_누적된다() {
    Long memberId = 1L;
    Level level = Level.createDefault(memberId);
    given(levelHistoryRepository.existsByOrderId(any())).willReturn(false);
    given(levelRepository.findWithLockByMemberId(memberId)).willReturn(Optional.of(level));

    levelService.recordPurchase(memberId, 100L, new BigDecimal("2000"));
    levelService.recordPurchase(memberId, 101L, new BigDecimal("3000"));

    assertThat(level.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("5000"));
  }

  @Test
  void getLevel_회원의_등급_정보를_LevelResponse로_반환한다() {
    Long memberId = 1L;
    Level level = Level.createDefault(memberId);
    given(levelRepository.findByMemberId(memberId)).willReturn(Optional.of(level));

    LevelResponse response = levelService.getLevel(memberId);

    assertThat(response.dishLevel()).isEqualTo(DishLevel.LEVEL_1);
    assertThat(response.purchaseCount()).isEqualTo(0);
    assertThat(response.remainToNextLevel()).isEqualTo(5);
  }

  @Test
  void getLevel_회원의_레벨이_없으면_기본값_기준으로_응답을_반환하고_저장하지_않는다() {
    Long memberId = 1L;
    given(levelRepository.findByMemberId(memberId)).willReturn(Optional.empty());

    LevelResponse response = levelService.getLevel(memberId);

    assertThat(response.dishLevel()).isEqualTo(DishLevel.LEVEL_1);
    assertThat(response.purchaseCount()).isEqualTo(0);
    verify(levelRepository, never()).save(any(Level.class));
  }

  @Test
  void getPointEarningRate_회원의_레벨이_없어도_기본_적립률을_반환하고_저장하지_않는다() {
    Long memberId = 1L;
    given(levelRepository.findByMemberId(memberId)).willReturn(Optional.empty());

    BigDecimal rate = levelService.getPointEarningRate(memberId);

    assertThat(rate).isEqualByComparingTo(DishLevel.LEVEL_1.getPointPercent());
    verify(levelRepository, never()).save(any(Level.class));
  }

  @Test
  void 구매를_20번_연속으로_하면_매_구매마다_이력이_기록되고_승급_시점에만_등급이_바뀐다() {
    Long memberId = 1L;
    Level level = Level.createDefault(memberId);
    given(levelHistoryRepository.existsByOrderId(any())).willReturn(false);
    given(levelRepository.findWithLockByMemberId(memberId)).willReturn(Optional.of(level));

    for (long orderId = 1; orderId <= 20; orderId++) {
      levelService.recordPurchase(memberId, orderId, BigDecimal.ZERO);
    }

    ArgumentCaptor<LevelHistory> captor = ArgumentCaptor.forClass(LevelHistory.class);
    verify(levelHistoryRepository, times(20)).save(captor.capture());

    List<LevelHistory> savedHistories = captor.getAllValues();
    assertThat(savedHistories).hasSize(20);

    // 승급 시점(5, 10, 15, 20번째)만 isUpgrade() == true
    int[] upgradeIndexes = {4, 9, 14, 19};
    DishLevel[] fromLevels = {
      DishLevel.LEVEL_1, DishLevel.LEVEL_2, DishLevel.LEVEL_3, DishLevel.LEVEL_4
    };
    DishLevel[] toLevels = {
      DishLevel.LEVEL_2, DishLevel.LEVEL_3, DishLevel.LEVEL_4, DishLevel.LEVEL_5
    };
    int[] counts = {5, 10, 15, 20};

    for (int i = 0; i < upgradeIndexes.length; i++) {
      LevelHistory upgradeHistory = savedHistories.get(upgradeIndexes[i]);
      assertThat(upgradeHistory.isUpgrade()).isTrue();
      assertThat(upgradeHistory.getOldLevel()).isEqualTo(fromLevels[i]);
      assertThat(upgradeHistory.getNewLevel()).isEqualTo(toLevels[i]);
      assertThat(upgradeHistory.getPurchaseCountAtChange()).isEqualTo(counts[i]);
    }

    for (int i = 0; i < savedHistories.size(); i++) {
      boolean isUpgradeIndex =
          i == upgradeIndexes[0]
              || i == upgradeIndexes[1]
              || i == upgradeIndexes[2]
              || i == upgradeIndexes[3];
      assertThat(savedHistories.get(i).isUpgrade()).isEqualTo(isUpgradeIndex);
      assertThat(savedHistories.get(i).getOrderId()).isEqualTo((long) (i + 1));
    }

    assertThat(level.getDishLevel()).isEqualTo(DishLevel.LEVEL_5);
    assertThat(level.getPurchaseCount()).isEqualTo(20);
  }
}
