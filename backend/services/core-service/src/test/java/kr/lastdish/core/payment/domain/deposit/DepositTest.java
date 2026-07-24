package kr.lastdish.core.payment.domain.deposit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DepositTest {

  @Test
  void 잔액이_충분하면_정상적으로_차감된다() {
    Deposit deposit = Deposit.createDefault(1L);
    deposit.refund(new BigDecimal("10000"));

    deposit.use(new BigDecimal("3000"));

    assertThat(deposit.getBalance()).isEqualByComparingTo(new BigDecimal("7000"));
  }

  @Test
  void 잔액보다_많은_금액을_사용하면_예외가_발생하고_잔액은_변하지_않는다() {
    Deposit deposit = Deposit.createDefault(1L);
    deposit.refund(new BigDecimal("5000"));

    assertThatThrownBy(() -> deposit.use(new BigDecimal("10000")))
        .isInstanceOf(InsufficientBalanceException.class);

    assertThat(deposit.getBalance()).isEqualByComparingTo(new BigDecimal("5000"));
  }

  @Test
  void 환불하면_잔액이_증가한다() {
    Deposit deposit = Deposit.createDefault(1L);

    deposit.refund(new BigDecimal("10000"));

    assertThat(deposit.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
  }

  @Test
  void 사용_금액이_0이하이면_예외가_발생한다() {
    Deposit deposit = Deposit.createDefault(1L);
    deposit.refund(new BigDecimal("10000"));

    assertThatThrownBy(() -> deposit.use(BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> deposit.use(new BigDecimal("-5000")))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(deposit.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
  }

  @Test
  void 환불_금액이_0이하이면_예외가_발생한다() {
    Deposit deposit = Deposit.createDefault(1L);
    deposit.refund(new BigDecimal("10000"));

    assertThatThrownBy(() -> deposit.refund(BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> deposit.refund(new BigDecimal("-3000")))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(deposit.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
  }

  @Test
  void 충전하면_잔액이_증가한다() {
    Deposit deposit = Deposit.createDefault(1L);

    deposit.charge(new BigDecimal("10000"));

    assertThat(deposit.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
  }

  @Test
  void 충전_금액이_0이하이면_예외가_발생한다() {
    Deposit deposit = Deposit.createDefault(1L);
    deposit.charge(new BigDecimal("10000"));

    assertThatThrownBy(() -> deposit.charge(BigDecimal.ZERO))
        .isInstanceOf(IllegalArgumentException.class);

    assertThat(deposit.getBalance()).isEqualByComparingTo(new BigDecimal("10000"));
  }
}
