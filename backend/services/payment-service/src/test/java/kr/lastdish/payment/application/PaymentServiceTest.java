package kr.lastdish.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import kr.lastdish.payment.application.dto.ApprovalClaim;
import kr.lastdish.payment.application.dto.PgApprovalResult;
import kr.lastdish.payment.application.event.ChargeRequestedEventWriter;
import kr.lastdish.payment.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentLogRepository paymentLogRepository;
  @Mock private ChargeRequestedEventWriter chargeRequestedEventWriter;

  @InjectMocks private PaymentService paymentService;
  @Captor private ArgumentCaptor<PaymentLog> paymentLogCaptor;

  @BeforeEach
  void setUpReconcileConfig() {
    ReflectionTestUtils.setField(paymentService, "processingVerifyThresholdMinutes", 40);
    ReflectionTestUtils.setField(paymentService, "processingVerifyLockTimeoutMinutes", 10);
    ReflectionTestUtils.setField(paymentService, "processingVerifyBatchSize", 50);
  }

  @Test
  void claimApproval_READY인_결제는_STARTED를_반환하고_PROCESSING으로_전환된다() {
    Payment payment = Payment.ready(1L, new BigDecimal("50000"), PgProvider.TOSS, "order-ready-1");

    when(paymentRepository.findWithLockByMerchantOrderId("order-ready-1"))
        .thenReturn(Optional.of(payment));

    ApprovalClaim claim = paymentService.claimApproval("order-ready-1", new BigDecimal("50000"));

    assertThat(claim.result()).isEqualTo(ApprovalClaimResult.STARTED);
    assertThat(claim.payment().getApprovedStatus()).isEqualTo(ApprovedStatus.PROCESSING);
  }

  @Test
  void claimApproval_이미_APPROVED된_결제는_ALREADY_APPROVED를_반환한다() {
    Payment approvedPayment =
        Payment.ready(1L, new BigDecimal("50000"), PgProvider.TOSS, "order-123");
    approvedPayment.claimApproval();
    approvedPayment.approve("toss-key-123");

    when(paymentRepository.findWithLockByMerchantOrderId("order-123"))
        .thenReturn(Optional.of(approvedPayment));

    ApprovalClaim claim = paymentService.claimApproval("order-123", new BigDecimal("50000"));

    assertThat(claim.result()).isEqualTo(ApprovalClaimResult.ALREADY_APPROVED);
  }

  @Test
  void claimApproval_이미_FAILED된_결제는_ALREADY_FAILED를_반환한다() {
    Payment failedPayment =
        Payment.ready(1L, new BigDecimal("50000"), PgProvider.TOSS, "order-456");
    failedPayment.claimApproval();
    failedPayment.fail();

    when(paymentRepository.findWithLockByMerchantOrderId("order-456"))
        .thenReturn(Optional.of(failedPayment));

    ApprovalClaim claim = paymentService.claimApproval("order-456", new BigDecimal("50000"));

    assertThat(claim.result()).isEqualTo(ApprovalClaimResult.ALREADY_FAILED);
  }

  @Test
  void claimApproval_EXPIRED된_결제는_예외가_발생한다() {
    Payment expiredPayment =
        Payment.ready(1L, new BigDecimal("50000"), PgProvider.TOSS, "order-expired-1");
    // 엔티티에 EXPIRED로 전이하는 public 메서드가 없어 테스트용으로 필드를 직접 세팅
    ReflectionTestUtils.setField(expiredPayment, "approvedStatus", ApprovedStatus.EXPIRED);

    when(paymentRepository.findWithLockByMerchantOrderId("order-expired-1"))
        .thenReturn(Optional.of(expiredPayment));

    assertThatThrownBy(
            () -> paymentService.claimApproval("order-expired-1", new BigDecimal("50000")))
        .isInstanceOf(PaymentException.class);
  }

  @Test
  void claimApproval_요청_금액이_다르면_예외가_발생한다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("50000"), PgProvider.TOSS, "order-amount-mismatch");

    when(paymentRepository.findWithLockByMerchantOrderId("order-amount-mismatch"))
        .thenReturn(Optional.of(payment));

    assertThatThrownBy(
            () -> paymentService.claimApproval("order-amount-mismatch", new BigDecimal("99999")))
        .isInstanceOf(PaymentException.class);
  }

  @Test
  void claimApproval_존재하지_않는_merchantOrderId면_예외가_발생한다() {
    when(paymentRepository.findWithLockByMerchantOrderId("not-exist")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentService.claimApproval("not-exist", new BigDecimal("50000")))
        .isInstanceOf(PaymentException.class);
  }

  @Test
  void approvePayment_호출시_승인반영과_PaymentLog저장과_Outbox이벤트발행이_모두_일어난다() {
    Payment payment =
        Payment.ready(1L, new BigDecimal("50000"), PgProvider.TOSS, "merchant-order-approve-1");
    payment.claimApproval(); // PROCESSING 상태로 만들어둬야 approve()가 통과됨
    when(paymentRepository.findWithLockById(1L)).thenReturn(Optional.of(payment));

    PgApprovalResult successResult =
        PgApprovalResult.success(
            "toss-key-approve-1", new BigDecimal("50000"), "카드", "1234", "issuer-code");

    Payment result = paymentService.approvePayment(1L, successResult);

    assertThat(result.getApprovedStatus()).isEqualTo(ApprovedStatus.APPROVED);
    assertThat(result.getPgTransactionId()).isEqualTo("toss-key-approve-1");

    verify(paymentLogRepository).save(paymentLogCaptor.capture());
    assertThat(paymentLogCaptor.getValue().getPaymentKey()).isEqualTo("toss-key-approve-1");

    verify(chargeRequestedEventWriter).append(any(Payment.class), anyLong());
  }

  @Test
  void approvePayment_존재하지_않는_paymentId면_예외가_발생한다() {
    when(paymentRepository.findWithLockById(999L)).thenReturn(Optional.empty());

    PgApprovalResult successResult =
        PgApprovalResult.success(
            "toss-key-x", new BigDecimal("50000"), "카드", "1234", "issuer-code");

    assertThatThrownBy(() -> paymentService.approvePayment(999L, successResult))
        .isInstanceOf(PaymentException.class);

    verify(chargeRequestedEventWriter, org.mockito.Mockito.never()).append(any(), anyLong());
  }

  @ParameterizedTest
  @CsvSource({
    "NOT_FOUND_PAYMENT_SESSION, 결제 시간이 만료되어 결제 진행 데이터가 존재하지 않습니다.",
    "NOT_FOUND_PAYMENT, 존재하지 않는 결제 정보입니다.",
    "INVALID_CARD_EXPIRATION, 카드 유효기간이 만료되었습니다.",
    "REJECT_CARD_COMPANY, 카드사에서 결제를 거절했습니다."
  })
  void failPayment_호출시_실패코드가_PaymentLog에_담겨_저장된다(String code, String message) {
    Payment payment =
        Payment.ready(1L, new BigDecimal("50000"), PgProvider.TOSS, "merchant-order-fail-1");
    payment.claimApproval();
    when(paymentRepository.findWithLockById(1L)).thenReturn(Optional.of(payment));

    PgApprovalResult failResult = PgApprovalResult.failure("test-payment-key-123", code, message);

    paymentService.failPayment(1L, failResult);

    verify(paymentLogRepository).save(paymentLogCaptor.capture());
    PaymentLog savedLog = paymentLogCaptor.getValue();

    assertThat(savedLog.getPaymentKey()).isEqualTo("test-payment-key-123");
    assertThat(savedLog.getFailedCode()).isEqualTo(code);
    assertThat(savedLog.getFailedMessage()).isEqualTo(message);
    assertThat(savedLog.getPaymentMethod()).isNull();
    assertThat(savedLog.getMaskedCardNum()).isNull();
    assertThat(savedLog.getCardCompany()).isNull();
  }

  @Test
  void claimStuckProcessingPayments_동일한_now로_클레임과_재조회를_수행한다() {
    Payment payment1 = mock(Payment.class);
    Payment payment2 = mock(Payment.class);

    when(paymentRepository.claimProcessingPayments(any(), any(), any(), anyInt())).thenReturn(2);
    when(paymentRepository.findClaimedProcessingPayments(any()))
            .thenReturn(List.of(payment1, payment2));

    List<Payment> result = paymentService.claimStuckProcessingPayments();

    assertThat(result).containsExactly(payment1, payment2);

    ArgumentCaptor<java.time.LocalDateTime> claimNowCaptor =
            ArgumentCaptor.forClass(java.time.LocalDateTime.class);
    verify(paymentRepository)
            .claimProcessingPayments(claimNowCaptor.capture(), any(), any(), anyInt());

    ArgumentCaptor<java.time.LocalDateTime> findNowCaptor =
            ArgumentCaptor.forClass(java.time.LocalDateTime.class);
    verify(paymentRepository).findClaimedProcessingPayments(findNowCaptor.capture());

    assertThat(claimNowCaptor.getValue()).isEqualTo(findNowCaptor.getValue());
  }

  @Test
  void claimStuckProcessingPayments_클레임된_건이_없으면_재조회하지_않고_빈_리스트를_반환한다() {
    when(paymentRepository.claimProcessingPayments(any(), any(), any(), anyInt())).thenReturn(0);

    List<Payment> result = paymentService.claimStuckProcessingPayments();

    assertThat(result).isEmpty();
    verify(paymentRepository, org.mockito.Mockito.never()).findClaimedProcessingPayments(any());
  }
}
