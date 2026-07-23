package kr.lastdish.core.payment.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentTest {

  @Test
  void READY_상태에서_승인하면_APPROVED로_변경된다() {

    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-1");

    payment.approve("toss-payment-key-1");

    assertThat(payment.getApprovedStatus()).isEqualTo(ApprovedStatus.APPROVED);
    assertThat(payment.getPgTransactionId()).isEqualTo("toss-payment-key-1");
    assertThat(payment.getApprovedAt()).isNotNull();
  }

  @Test
  void READY_상태에서_실패처리하면_FAILED로_변경된다() {

    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-2");

    payment.fail();

    assertThat(payment.getApprovedStatus()).isEqualTo(ApprovedStatus.FAILED);
  }

  @Test
  void 이미_APPROVED된_결제를_또_승인하려하면_예외가_발생한다() {

    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-3");
    payment.approve("toss-payment-key-3"); // 먼저 승인 완료 상태로 만듦

    assertThatThrownBy(() -> payment.approve("toss-payment-key-3-retry"))
        .isInstanceOf(PaymentException.class);
  }

  @Test
  void 이미_FAILED된_결제를_승인하려하면_예외가_발생한다() {

    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-4");
    payment.fail();

    assertThatThrownBy(() -> payment.approve("toss-payment-key-4"))
        .isInstanceOf(PaymentException.class);
  }

  @Test
  void 이미_APPROVED된_결제를_실패처리하려하면_예외가_발생한다() {

    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-5");
    payment.approve("toss-payment-key-5");

    assertThatThrownBy(payment::fail).isInstanceOf(PaymentException.class);
  }

  @Test
  void 이미_APPROVED된_결제를_또_승인해도_기존_pgTransactionId는_바뀌지_않는다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-6");
    payment.approve("original-key");

    assertThatThrownBy(() -> payment.approve("attacker-key")).isInstanceOf(PaymentException.class);

    assertThat(payment.getPgTransactionId()).isEqualTo("original-key");
  }
}
