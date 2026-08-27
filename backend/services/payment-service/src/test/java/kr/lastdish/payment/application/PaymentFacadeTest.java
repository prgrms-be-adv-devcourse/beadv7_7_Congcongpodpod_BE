package kr.lastdish.payment.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.payment.application.dto.PgApprovalResult;
import kr.lastdish.payment.application.port.PgPaymentGateway;
import kr.lastdish.payment.domain.Payment;
import kr.lastdish.payment.domain.PaymentException;
import kr.lastdish.payment.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @Mock private PaymentService paymentService;
    @Mock private PgPaymentGateway pgPaymentGateway;

    @InjectMocks private PaymentFacade paymentFacade;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentFacade, "verifyDelayMs", 0L);
    }

    @Test
    void verifyPaymentStatus_SUCCESS면_approvePayment을_호출한다() {
        Payment payment = mock(Payment.class);
        given(payment.getId()).willReturn(1L);
        given(payment.getMerchantOrderId()).willReturn("order-1");
        given(paymentService.claimStuckProcessingPayments()).willReturn(List.of(payment));
        given(pgPaymentGateway.checkStatus("order-1"))
                .willReturn(PgApprovalResult.success("pk_1", BigDecimal.valueOf(10000), "카드", null, null));

        paymentFacade.verifyProcessingPayments();

        verify(paymentService).approvePayment(eq(1L), any(PgApprovalResult.class));
        verify(paymentService, never()).failPayment(any(), any());
    }

    @Test
    void verifyPaymentStatus_FAILURE면_failPayment을_호출한다() {
        Payment payment = mock(Payment.class);
        given(payment.getId()).willReturn(1L);
        given(payment.getMerchantOrderId()).willReturn("order-1");
        given(paymentService.claimStuckProcessingPayments()).willReturn(List.of(payment));
        given(pgPaymentGateway.checkStatus("order-1"))
                .willReturn(PgApprovalResult.failure(null, "NOT_FOUND_AFTER_THRESHOLD", "..."));

        paymentFacade.verifyProcessingPayments();

        verify(paymentService).failPayment(eq(1L), any(PgApprovalResult.class));
        verify(paymentService, never()).approvePayment(any(), any());
    }

    @Test
    void verifyPaymentStatus_UNKNOWN이면_아무것도_확정하지_않는다() {
        Payment payment = mock(Payment.class);
        given(payment.getMerchantOrderId()).willReturn("order-1");
        given(paymentService.claimStuckProcessingPayments()).willReturn(List.of(payment));
        given(pgPaymentGateway.checkStatus("order-1"))
                .willReturn(PgApprovalResult.unknown(null, "..."));

        paymentFacade.verifyProcessingPayments();

        verify(paymentService, never()).approvePayment(any(), any());
        verify(paymentService, never()).failPayment(any(), any());
    }

    @Test
    void verifyPaymentStatus_한_건이_PaymentException을_던져도_나머지_건은_계속_처리한다() {
        Payment failing = mock(Payment.class);
        given(failing.getId()).willReturn(1L);
        given(failing.getMerchantOrderId()).willReturn("order-1");
        Payment succeeding = mock(Payment.class);
        given(succeeding.getId()).willReturn(2L);
        given(succeeding.getMerchantOrderId()).willReturn("order-2");

        given(paymentService.claimStuckProcessingPayments()).willReturn(List.of(failing, succeeding));
        given(pgPaymentGateway.checkStatus("order-1"))
                .willReturn(PgApprovalResult.success("pk_1", BigDecimal.TEN, "카드", null, null));
        willThrow(new PaymentException(ErrorCode.INVALID_PAYMENT_STATUS, "이미 처리됨"))
                .given(paymentService)
                .approvePayment(eq(1L), any());
        given(pgPaymentGateway.checkStatus("order-2"))
                .willReturn(PgApprovalResult.success("pk_2", BigDecimal.TEN, "카드", null, null));

        paymentFacade.verifyProcessingPayments();

        verify(paymentService).approvePayment(eq(2L), any());
    }

    @Test
    void verifyProcessingPayments_클레임된_건이_없으면_아무것도_호출하지_않는다() {
        given(paymentService.claimStuckProcessingPayments()).willReturn(List.of());

        paymentFacade.verifyProcessingPayments();

        verify(pgPaymentGateway, never()).checkStatus(any());
        verify(paymentService, never()).approvePayment(any(), any());
        verify(paymentService, never()).failPayment(any(), any());
    }
}