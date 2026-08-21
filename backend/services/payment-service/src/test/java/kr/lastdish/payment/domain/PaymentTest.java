package kr.lastdish.payment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentTest {

  @Test
  void claimApproval_READY_상태에서_호출하면_PROCESSING으로_전환되고_STARTED를_반환한다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-claim-1");

    ApprovalClaimResult result = payment.claimApproval();

    assertThat(result).isEqualTo(ApprovalClaimResult.STARTED);
    assertThat(payment.getApprovedStatus()).isEqualTo(ApprovedStatus.PROCESSING);
  }

  @Test
  void claimApproval_PROCESSING_상태에서_호출하면_ALREADY_PROCESSING을_반환하고_상태는_유지된다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-claim-2");
    payment.claimApproval(); // READY -> PROCESSING

    ApprovalClaimResult result = payment.claimApproval(); // 두 번째 선점 시도

    assertThat(result).isEqualTo(ApprovalClaimResult.ALREADY_PROCESSING);
    assertThat(payment.getApprovedStatus()).isEqualTo(ApprovedStatus.PROCESSING);
  }

  @Test
  void claimApproval_APPROVED_상태에서_호출하면_ALREADY_APPROVED를_반환한다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-claim-3");
    payment.claimApproval();
    payment.approve("toss-payment-key-claim-3");

    ApprovalClaimResult result = payment.claimApproval();

    assertThat(result).isEqualTo(ApprovalClaimResult.ALREADY_APPROVED);
  }

  @Test
  void claimApproval_FAILED_상태에서_호출하면_ALREADY_FAILED를_반환한다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-claim-4");
    payment.claimApproval();
    payment.fail();

    ApprovalClaimResult result = payment.claimApproval();

    assertThat(result).isEqualTo(ApprovalClaimResult.ALREADY_FAILED);
  }

  @Test
  void READY_상태에서_바로_승인하려하면_예외가_발생한다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-7");

    // claimApproval()로 PROCESSING을 거치지 않고 바로 approve() 호출 -> 예외
    assertThatThrownBy(() -> payment.approve("toss-payment-key-7"))
        .isInstanceOf(PaymentException.class);
  }

  @Test
  void READY_상태에서_바로_실패처리하려하면_예외가_발생한다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-8");

    assertThatThrownBy(payment::fail).isInstanceOf(PaymentException.class);
  }

  @Test
  void PROCESSING_상태에서_승인하면_APPROVED로_변경된다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-1");
    payment.claimApproval(); // READY -> PROCESSING

    payment.approve("toss-payment-key-1");

    assertThat(payment.getApprovedStatus()).isEqualTo(ApprovedStatus.APPROVED);
    assertThat(payment.getPgTransactionId()).isEqualTo("toss-payment-key-1");
    assertThat(payment.getApprovedAt()).isNotNull();
  }

  @Test
  void PROCESSING_상태에서_실패처리하면_FAILED로_변경된다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-2");
    payment.claimApproval();

    payment.fail();

    assertThat(payment.getApprovedStatus()).isEqualTo(ApprovedStatus.FAILED);
  }

  @Test
  void 이미_APPROVED된_결제를_또_승인하려하면_예외가_발생한다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-3");
    payment.claimApproval();
    payment.approve("toss-payment-key-3"); // 먼저 승인 완료 상태로 만듦

    assertThatThrownBy(() -> payment.approve("toss-payment-key-3-retry"))
        .isInstanceOf(PaymentException.class);
  }

  @Test
  void 이미_FAILED된_결제를_승인하려하면_예외가_발생한다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-4");
    payment.claimApproval();
    payment.fail();

    assertThatThrownBy(() -> payment.approve("toss-payment-key-4"))
        .isInstanceOf(PaymentException.class);
  }

  @Test
  void 이미_APPROVED된_결제를_실패처리하려하면_예외가_발생한다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-5");
    payment.claimApproval();
    payment.approve("toss-payment-key-5");

    assertThatThrownBy(payment::fail).isInstanceOf(PaymentException.class);
  }

  @Test
  void 이미_APPROVED된_결제를_또_승인해도_기존_pgTransactionId는_바뀌지_않는다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("10000"), PgProvider.TOSS, "merchant-order-id-6");
    payment.claimApproval();
    payment.approve("original-key");

    assertThatThrownBy(() -> payment.approve("attacker-key")).isInstanceOf(PaymentException.class);

    assertThat(payment.getPgTransactionId()).isEqualTo("original-key");
  }
}
