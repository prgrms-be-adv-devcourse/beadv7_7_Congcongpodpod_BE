package kr.lastdish.core.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import kr.lastdish.core.payment.application.dto.DepositTransactionResult;
import kr.lastdish.core.payment.domain.deposit.Deposit;
import kr.lastdish.core.payment.domain.deposit.DepositHistory;
import kr.lastdish.core.payment.domain.deposit.DepositNotFoundException;
import kr.lastdish.core.payment.domain.deposit.InsufficientBalanceException;
import kr.lastdish.core.payment.infrastructure.DepositHistoryRepository;
import kr.lastdish.core.payment.infrastructure.DepositRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepositServiceTest {

  @Mock private DepositRepository depositRepository;
  @Mock private DepositHistoryRepository depositHistoryRepository;

  @InjectMocks private DepositService depositService;

  private Deposit deposit;

  @BeforeEach
  void setUp() {
    deposit = Deposit.createDefault(123L);
    deposit.refund(new BigDecimal("10000")); // 잔액 10000원인 상태로 시작
  }

  @Test
  void 예치금_사용에_성공하면_잔액이_차감되고_이력이_저장된다() {
    // given
    when(depositRepository.findWithLockByMemberId(123L)).thenReturn(Optional.of(deposit));
    when(depositHistoryRepository.save(any(DepositHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0)); // 저장 흉내: 넘어온 값 그대로 반환

    // when
    DepositTransactionResult result = depositService.use(123L, 999L, new BigDecimal("3000"));

    // then
    assertThat(result.balanceAfter()).isEqualByComparingTo(new BigDecimal("7000"));
    verify(depositHistoryRepository).save(any(DepositHistory.class)); // 이력 저장이 실제로 호출됐는지 확인
  }

  @Test
  void 잔액이_부족하면_예치금_사용이_실패한다() {
    // given
    when(depositRepository.findWithLockByMemberId(123L)).thenReturn(Optional.of(deposit));

    // when & then
    assertThatThrownBy(() -> depositService.use(123L, 999L, new BigDecimal("999999")))
        .isInstanceOf(InsufficientBalanceException.class);
  }

  @Test
  void 계좌가_없는_회원이_사용을_시도하면_예외가_발생한다() {
    // given
    when(depositRepository.findWithLockByMemberId(456L)).thenReturn(Optional.empty());

    // when & then
    assertThatThrownBy(() -> depositService.use(456L, 999L, new BigDecimal("1000")))
        .isInstanceOf(DepositNotFoundException.class);
  }

  @Test
  void 예치금_환불에_성공하면_잔액이_증가하고_이력이_저장된다() {
    // given
    when(depositRepository.findWithLockByMemberId(123L)).thenReturn(Optional.of(deposit));
    when(depositHistoryRepository.save(any(DepositHistory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // when
    DepositTransactionResult result = depositService.refund(123L, 999L, new BigDecimal("2000"));

    // then
    assertThat(result.balanceAfter()).isEqualByComparingTo(new BigDecimal("12000"));
    verify(depositHistoryRepository).save(any(DepositHistory.class));
  }
}
