package kr.lastdish.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import kr.lastdish.payment.application.dto.PgApprovalResult;
import kr.lastdish.payment.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentLogRepository paymentLogRepository;

  @InjectMocks private PaymentService paymentService;
  @Captor private ArgumentCaptor<PaymentLog> paymentLogCaptor;

  @Test
  void 이미_APPROVED된_결제는_getReadyPayment에서_예외가_발생한다() {
    Payment approvedPayment =
        Payment.ready(1L, new BigDecimal("50000"), PgProvider.TOSS, "order-123");
    approvedPayment.approve("toss-key-123");

    when(paymentRepository.findWithLockByMerchantOrderId("order-123"))
        .thenReturn(Optional.of(approvedPayment));

    assertThatThrownBy(() -> paymentService.getReadyPayment("order-123", new BigDecimal("50000")))
        .isInstanceOf(PaymentException.class)
        .satisfies(
            e -> {
              PaymentException ex = (PaymentException) e;
              assertThat(ex.getErrorCode().getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });
  }

  @Test
  void FAILED_상태인_결제도_getReadyPayment에서_예외가_발생한다() {
    Payment failedPayment =
        Payment.ready(1L, new BigDecimal("50000"), PgProvider.TOSS, "order-456");
    failedPayment.fail();

    when(paymentRepository.findWithLockByMerchantOrderId("order-456"))
        .thenReturn(Optional.of(failedPayment));

    assertThatThrownBy(() -> paymentService.getReadyPayment("order-456", new BigDecimal("50000")))
        .isInstanceOf(PaymentException.class);
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
    when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

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
}
