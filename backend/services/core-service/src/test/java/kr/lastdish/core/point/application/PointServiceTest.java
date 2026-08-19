package kr.lastdish.core.point.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.level.application.LevelService;
import kr.lastdish.core.point.application.dto.PointTransactionResult;
import kr.lastdish.core.point.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {

  @Mock private PointRepository pointRepository;
  @Mock private PointHistoryRepository pointHistoryRepository;
  @Mock private LevelService levelService;

  @InjectMocks private PointService pointService;

  @Test
  void getOrDefaultPoint_기존_포인트가_없으면_기본값을_반환하고_저장하지_않는다() {
    given(pointRepository.findByMemberId(1L)).willReturn(Optional.empty());

    Point result = pointService.getOrDefaultPoint(1L);

    assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    verify(pointRepository, never()).save(any());
  }

  @Test
  void getOrDefaultPoint_기존_포인트가_있으면_그대로_반환한다() {
    Point existing = Point.createDefault(1L);
    given(pointRepository.findByMemberId(1L)).willReturn(Optional.of(existing));

    Point result = pointService.getOrDefaultPoint(1L);

    assertThat(result).isEqualTo(existing);
    verify(pointRepository, never()).save(any());
  }

  @Test
  void earn_호출하면_적립률만큼_계산되어_잔액이_증가하고_이력이_기록된다() {
    Point point = Point.createDefault(1L);
    given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.of(point));
    given(levelService.getPointEarningRate(1L)).willReturn(new BigDecimal("0.05"));
    given(pointHistoryRepository.save(any(PointHistory.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    PointTransactionResult result = pointService.earn(1L, 100L, new BigDecimal("10000"));

    assertThat(point.getBalance()).isEqualByComparingTo(new BigDecimal("500"));
    assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("500"));

    ArgumentCaptor<PointHistory> captor = ArgumentCaptor.forClass(PointHistory.class);
    verify(pointHistoryRepository).save(captor.capture());
    assertThat(captor.getValue().getType()).isEqualTo(PointType.EARN);
  }

  @Test
  void earn_호출_시_createDefaultIfAbsent를_먼저_호출한다() {
    Point point = Point.createDefault(1L);
    given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.of(point));
    given(levelService.getPointEarningRate(1L)).willReturn(new BigDecimal("0.05"));
    given(pointHistoryRepository.save(any(PointHistory.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    pointService.earn(1L, 100L, new BigDecimal("10000"));

    verify(pointRepository, times(1)).createDefaultIfAbsent(1L);
    verify(pointRepository, never()).save(any(Point.class));
  }

  @Test
  void earn_주문금액이_null이면_예외가_발생한다() {
    assertThatThrownBy(() -> pointService.earn(1L, 100L, null))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void earn_주문금액이_0이하이면_예외가_발생한다() {
    assertThatThrownBy(() -> pointService.earn(1L, 100L, BigDecimal.ZERO))
        .isInstanceOf(BusinessException.class);

    assertThatThrownBy(() -> pointService.earn(1L, 100L, new BigDecimal("-100")))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  void use_포인트_정보가_없는_회원이면_예외가_발생한다() {
    given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> pointService.use(1L, 200L, new BigDecimal("100")))
        .isInstanceOf(PointNotFoundException.class);
  }

  @Test
  void use_잔액이_부족하면_예외가_발생하고_이력이_기록되지_않는다() {
    Point point = Point.createDefault(1L);
    point.earn(new BigDecimal("100"));
    given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.of(point));

    assertThatThrownBy(() -> pointService.use(1L, 200L, new BigDecimal("500")))
        .isInstanceOf(InsufficientPointException.class);

    verify(pointHistoryRepository, never()).save(any());
  }

  @Test
  void use_단일_적립건으로_충분하면_해당_건만_차감된다() {
    Point point = Point.createDefault(1L);
    point.earn(new BigDecimal("1000"));
    given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.of(point));

    PointHistory earnHistory =
        PointHistory.recordEarn(1L, 100L, new BigDecimal("1000"), new BigDecimal("1000"));
    given(pointHistoryRepository.findUsableEarnHistories(1L)).willReturn(List.of(earnHistory));
    given(pointHistoryRepository.save(any(PointHistory.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    pointService.use(1L, 200L, new BigDecimal("300"));

    assertThat(earnHistory.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("700"));
    assertThat(point.getBalance()).isEqualByComparingTo(new BigDecimal("700"));
  }

  @Test
  void use_여러_적립건에_걸쳐있으면_오래된_건부터_FIFO로_소진된다() {
    Point point = Point.createDefault(1L);
    point.earn(new BigDecimal("1000"));
    given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.of(point));

    PointHistory oldEarn =
        PointHistory.recordEarn(1L, 100L, new BigDecimal("300"), new BigDecimal("300"));
    PointHistory newEarn =
        PointHistory.recordEarn(1L, 101L, new BigDecimal("700"), new BigDecimal("1000"));
    given(pointHistoryRepository.findUsableEarnHistories(1L)).willReturn(List.of(oldEarn, newEarn));
    given(pointHistoryRepository.save(any(PointHistory.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    pointService.use(1L, 200L, new BigDecimal("500"));

    assertThat(oldEarn.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(newEarn.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("500"));
  }

  @Test
  void use_적립이력_합계보다_많이_소진하려하면_정합성_예외가_발생한다() {
    Point point = Point.createDefault(1L);
    point.earn(new BigDecimal("1000"));
    given(pointRepository.findWithLockByMemberId(1L)).willReturn(Optional.of(point));

    PointHistory earnHistory =
        PointHistory.recordEarn(1L, 100L, new BigDecimal("500"), new BigDecimal("500"));
    given(pointHistoryRepository.findUsableEarnHistories(1L)).willReturn(List.of(earnHistory));

    assertThatThrownBy(() -> pointService.use(1L, 200L, new BigDecimal("800")))
        .isInstanceOf(IllegalStateException.class);
  }
}
