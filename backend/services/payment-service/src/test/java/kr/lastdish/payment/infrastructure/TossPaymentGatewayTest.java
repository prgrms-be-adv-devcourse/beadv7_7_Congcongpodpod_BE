package kr.lastdish.payment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.math.BigDecimal;
import kr.lastdish.payment.application.dto.PgApprovalResult;
import kr.lastdish.payment.domain.LogType;
import kr.lastdish.payment.domain.PaymentLog;
import kr.lastdish.payment.domain.PaymentLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;


@ExtendWith(MockitoExtension.class)
class TossPaymentGatewayTest {

    @Mock private PaymentLogRepository paymentLogRepository;

    private MockRestServiceServer mockServer;
    private TossPaymentGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        gateway = new TossPaymentGateway(builder, paymentLogRepository);
        ReflectionTestUtils.setField(gateway, "secretKey", "test_secret_key");
    }

    @Test
    void approve_정상승인시_SUCCESS를_반환한다() {
        mockServer
                .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess(
                                """
                                {"paymentKey":"pk_1","orderId":"order_1","totalAmount":10000,
                                 "status":"DONE","method":"카드","card":{"number":"1234","issuerCode":"3K"}}
                                """,
                                MediaType.APPLICATION_JSON));

        PgApprovalResult result = gateway.approve(1L, "pk_1", "order_1", BigDecimal.valueOf(10000));

        assertThat(result.status()).isEqualTo(PgApprovalResult.Status.SUCCESS);
        assertThat(result.pgTransactionId()).isEqualTo("pk_1");
        mockServer.verify();
    }

    @Test
    void approve_Toss가_명확히_거절하면_FAILURE를_반환한다() {
        mockServer
                .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST)
                                .contentType(MediaType.APPLICATION_JSON)
                                .body("{\"code\":\"REJECT_CARD_COMPANY\",\"message\":\"카드사 거절\"}"));

        PgApprovalResult result = gateway.approve(1L, "pk_2", "order_2", BigDecimal.valueOf(10000));

        assertThat(result.status()).isEqualTo(PgApprovalResult.Status.FAILURE);
        assertThat(result.failureCode()).isEqualTo("REJECT_CARD_COMPANY");
        mockServer.verify();
    }

    @Test
    void approve_confirm이_타임아웃되고_조회API가_DONE이면_SUCCESS로_복구한다() {
        mockServer
                .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(request -> { throw new IOException("simulated timeout"); });

        mockServer
                .expect(requestTo("https://api.tosspayments.com/v1/payments/pk_3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                {"paymentKey":"pk_3","orderId":"order_3","totalAmount":10000,"status":"DONE","method":"카드"}
                                """,
                                MediaType.APPLICATION_JSON));

        PgApprovalResult result = gateway.approve(1L, "pk_3", "order_3", BigDecimal.valueOf(10000));

        assertThat(result.status()).isEqualTo(PgApprovalResult.Status.SUCCESS);
        mockServer.verify();
    }

    @Test
    void approve_confirm과_조회_둘다_실패하면_UNKNOWN을_반환하고_PaymentLog를_남긴다() {
        mockServer
                .expect(requestTo("https://api.tosspayments.com/v1/payments/confirm"))
                .andRespond(request -> { throw new IOException("simulated timeout"); });

        mockServer
                .expect(requestTo("https://api.tosspayments.com/v1/payments/pk_4"))
                .andRespond(request -> { throw new IOException("simulated timeout"); });

        mockServer
                .expect(requestTo("https://api.tosspayments.com/v1/payments/pk_4"))
                .andRespond(request -> { throw new IOException("simulated timeout"); });

        PgApprovalResult result = gateway.approve(1L, "pk_4", "order_4", BigDecimal.valueOf(10000));

        assertThat(result.status()).isEqualTo(PgApprovalResult.Status.UNKNOWN);

        ArgumentCaptor<PaymentLog> captor = ArgumentCaptor.forClass(PaymentLog.class);
        verify(paymentLogRepository, times(2)).save(captor.capture());

        PaymentLog lastLog = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(lastLog.getLogType()).isEqualTo(LogType.RESPONSE);
        assertThat(lastLog.getFailedCode()).isEqualTo("UNKNOWN");
        assertThat(lastLog.getPaymentKey()).isEqualTo("pk_4");

        mockServer.verify();
    }
}