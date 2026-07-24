package kr.lastdish.core.payment.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import kr.lastdish.core.payment.application.dto.PgApprovalResult;
import kr.lastdish.core.payment.application.port.PgPaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class TossPaymentGateway implements PgPaymentGateway {

  private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

  @Value("${toss.secret-key:test_gsk_docs_OaPz8L5KdmQXkzRz3y47BMw6}")
  private String secretKey;

  private final RestClient restClient = buildRestClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static RestClient buildRestClient() {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(3000);
    requestFactory.setReadTimeout(5000);

    return RestClient.builder().requestFactory(requestFactory).build();
  }

  @Override
  public PgApprovalResult approve(String paymentKey, String orderId, BigDecimal amount) {
    try {
      String rawJson =
          restClient
              .post()
              .uri(TOSS_CONFIRM_URL)
              .header("Authorization", buildAuthorizationHeader())
              .header("Content-Type", "application/json")
              .body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
              .retrieve()
              .body(String.class);

      TossConfirmResponse response = objectMapper.readValue(rawJson, TossConfirmResponse.class);

      return PgApprovalResult.success(response.paymentKey(), response.totalAmount(), rawJson);

    } catch (RestClientResponseException e) {
      String rawJson = e.getResponseBodyAsString();
      TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
      String code = error != null && error.code() != null ? error.code() : "UNKNOWN_ERROR";
      String message = error != null && error.message() != null ? error.message() : e.getMessage();
      return PgApprovalResult.failure(code, message, rawJson);

    } catch (Exception e) {
      return PgApprovalResult.failure("NETWORK_ERROR", "결제 서버와의 통신에 실패했습니다.", null);
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
