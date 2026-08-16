package kr.lastdish.core.level.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
  void getOrCreateLevel_기존_레벨이_없으면_기본값으로_생성한다() {
    Long memberId = 1L;
    given(levelRepository.findByMemberId(memberId)).willReturn(Optional.empty());
    given(levelRepository.save(any(Level.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    Level result = levelService.getOrCreateLevel(memberId);

    assertThat(result.getDishLevel()).isEqualTo(DishLevel.LEVEL_1);
    verify(levelRepository, times(1)).save(any(Level.class));
  }

  @Test
  void getOrCreateLevel_기존_레벨이_있으면_그대로_반환한다() {
    Long memberId = 1L;
    Level existing = Level.createDefault(memberId);
    given(levelRepository.findByMemberId(memberId)).willReturn(Optional.of(existing));

    Level result = levelService.getOrCreateLevel(memberId);

    assertThat(result).isEqualTo(existing);
    verify(levelRepository, never()).save(any(Level.class));
  }

  @Test
  void recordPurchase_승급하면_LevelHistory가_저장된다() {
    Long memberId = 1L;
    Level level = Level.createDefault(memberId);
    for (int i = 0; i < 4; i++) {
      level.addPurchase(); // 구매횟수 4로 세팅 (이번 구매로 5 -> 승급)
    }
    given(levelRepository.findWithLockByMemberId(memberId)).willReturn(Optional.of(level));

    levelService.recordPurchase(memberId, new BigDecimal("1000")); // 수정됨 - discountAmount 파라미터 추가

    verify(levelHistoryRepository, times(1)).save(any(LevelHistory.class));
  }

  @Test
  void recordPurchase_승급하지_않으면_LevelHistory가_저장되지_않는다() {
    Long memberId = 1L;
    Level level = Level.createDefault(memberId); // 구매횟수 0

    given(levelRepository.findWithLockByMemberId(memberId)).willReturn(Optional.of(level));

    levelService.recordPurchase(memberId, new BigDecimal("1000")); // 수정됨

    verify(levelHistoryRepository, never()).save(any(LevelHistory.class));
  }

  // 아래부터 추가됨
  @Test
  void recordPurchase_호출하면_할인금액이_누적된다() {
    Long memberId = 1L;
    Level level = Level.createDefault(memberId);
    given(levelRepository.findWithLockByMemberId(memberId)).willReturn(Optional.of(level));

    levelService.recordPurchase(memberId, new BigDecimal("2000"));
    levelService.recordPurchase(memberId, new BigDecimal("3000"));

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
    assertThat(response.remainToNextLevel()).isEqualTo(5); // LEVEL_2까지 5회 남음
  }

  @Test
  void 구매를_20번_연속으로_하면_승급_시점에만_이력이_정확히_기록된다() {
    Long memberId = 1L;
    Level level = Level.createDefault(memberId);
    given(levelRepository.findWithLockByMemberId(memberId)).willReturn(Optional.of(level));

    // 1회 ~ 20회까지 연속 구매
    for (int i = 1; i <= 20; i++) {
      levelService.recordPurchase(memberId, BigDecimal.ZERO);
    }

    // 승급은 5, 10, 15, 20회 시점에 총 4번만 발생해야 함
    ArgumentCaptor<LevelHistory> captor = ArgumentCaptor.forClass(LevelHistory.class);
    verify(levelHistoryRepository, times(4)).save(captor.capture());

    List<LevelHistory> savedHistories = captor.getAllValues();

    assertThat(savedHistories).hasSize(4);
    assertThat(savedHistories.get(0).getOldLevel()).isEqualTo(DishLevel.LEVEL_1);
    assertThat(savedHistories.get(0).getNewLevel()).isEqualTo(DishLevel.LEVEL_2);
    assertThat(savedHistories.get(0).getPurchaseCountAtChange()).isEqualTo(5);

    assertThat(savedHistories.get(1).getOldLevel()).isEqualTo(DishLevel.LEVEL_2);
    assertThat(savedHistories.get(1).getNewLevel()).isEqualTo(DishLevel.LEVEL_3);
    assertThat(savedHistories.get(1).getPurchaseCountAtChange()).isEqualTo(10);

    assertThat(savedHistories.get(2).getOldLevel()).isEqualTo(DishLevel.LEVEL_3);
    assertThat(savedHistories.get(2).getNewLevel()).isEqualTo(DishLevel.LEVEL_4);
    assertThat(savedHistories.get(2).getPurchaseCountAtChange()).isEqualTo(15);

    assertThat(savedHistories.get(3).getOldLevel()).isEqualTo(DishLevel.LEVEL_4);
    assertThat(savedHistories.get(3).getNewLevel()).isEqualTo(DishLevel.LEVEL_5);
    assertThat(savedHistories.get(3).getPurchaseCountAtChange()).isEqualTo(20);

    // 최종 상태 확인
    assertThat(level.getDishLevel()).isEqualTo(DishLevel.LEVEL_5);
    assertThat(level.getPurchaseCount()).isEqualTo(20);
  }
}
