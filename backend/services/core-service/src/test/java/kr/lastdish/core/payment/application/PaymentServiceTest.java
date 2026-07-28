package kr.lastdish.core.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import kr.lastdish.core.payment.application.payment.PaymentService;
import kr.lastdish.core.payment.domain.payment.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private PaymentLogRepository paymentLogRepository;

  @InjectMocks private PaymentService paymentService;

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
}
