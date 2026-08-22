package kr.lastdish.payment.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import kr.lastdish.payment.application.dto.PgApprovalResult;
import kr.lastdish.payment.application.port.PgPaymentGateway;
import kr.lastdish.payment.domain.PaymentLog;
import kr.lastdish.payment.domain.PaymentLogRepository;
import kr.lastdish.payment.domain.PgProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TossPaymentGateway implements PgPaymentGateway {

  private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
  private static final int MAX_FAILURE_MESSAGE_LENGTH = 500;
  private final PaymentLogRepository paymentLogRepository;

  @Value("${toss.secret-key:test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6}")
  private String secretKey;

  private final RestClient restClient = buildRestClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  // failed_message 컬럼 길이를 넘지 않도록 예외 메시지를 자른다
  private static String truncate(String value, int maxLength) {
    return (value == null || value.length() <= maxLength) ? value : value.substring(0, maxLength);
  }

  private static RestClient buildRestClient() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(3000);
    requestFactory.setReadTimeout(5000);

    return RestClient.builder().requestFactory(requestFactory).build();
  }

  @Override
  public PgApprovalResult approve(
      Long paymentId, String paymentKey, String orderId, BigDecimal amount) {

    // REQUEST 로그 기록
    paymentLogRepository.save(PaymentLog.createRequestLog(paymentId, PgProvider.TOSS, paymentKey));

    try {
      Map<String, Object> requestBody =
          Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount);
      log.debug("Toss로 결제 승인 요청. orderId={}, amount={}", orderId, amount);
      String rawJson =
          restClient
              .post()
              .uri(TOSS_CONFIRM_URL)
              .header("Authorization", buildAuthorizationHeader())
              .header("Content-Type", "application/json")
              .body(requestBody)
              .retrieve()
              .body(String.class);

      TossConfirmResponse response = objectMapper.readValue(rawJson, TossConfirmResponse.class);

      return PgApprovalResult.success(
          response.paymentKey(),
          response.totalAmount(),
          response.method(),
          response.card() != null ? response.card().number() : null,
          response.card() != null ? response.card().issuerCode() : null);

    } catch (RestClientResponseException e) {
      TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
      String code = error != null && error.code() != null ? error.code() : "UNKNOWN_ERROR";
      String message = error != null && error.message() != null ? error.message() : e.getMessage();

      return PgApprovalResult.failure(
          paymentKey, code, truncate(message, MAX_FAILURE_MESSAGE_LENGTH));

    } catch (Exception e) {
      log.error("Toss 통신 중 예상치 못한 예외 발생. paymentId={}, orderId={}", paymentId, orderId, e);
      return PgApprovalResult.failure(paymentKey, "NETWORK_ERROR", "결제 서버와의 통신에 실패했습니다.");
    }
  }

  // 시크릿 키와 콜론(:)을 Base64로 인코딩하여 Basic 인증 헤더 생성
  private String buildAuthorizationHeader() {
    String credentials = secretKey + ":";
    String encoded =
        Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    return "Basic " + encoded;
  }
}
