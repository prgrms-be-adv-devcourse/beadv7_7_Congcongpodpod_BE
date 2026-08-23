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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
public class TossPaymentGateway implements PgPaymentGateway {

  private static final String TOSS_CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";
  private static final String TOSS_PAYMENT_INQUIRY_URL = "https://api.tosspayments.com/v1/payments/";
  private static final int MAX_FAILURE_MESSAGE_LENGTH = 500;
  private static final long INQUIRY_RETRY_DELAY_MS = 700L;

  private final PaymentLogRepository paymentLogRepository;
  private final RestClient restClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Value("${toss.secret-key}")
  private String secretKey;

  public TossPaymentGateway(
          @Qualifier("tossRestClientBuilder") RestClient.Builder restClientBuilder,
          PaymentLogRepository paymentLogRepository) {
    this.paymentLogRepository = paymentLogRepository;
    this.restClient = restClientBuilder.build();
  }
  // failed_message 컬럼 길이를 넘지 않도록 예외 메시지를 자른다
  private static String truncate(String value, int maxLength) {
    return (value == null || value.length() <= maxLength) ? value : value.substring(0, maxLength);
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
      // Toss 거절 응답
      TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
      String code = error != null && error.code() != null ? error.code() : "UNKNOWN_ERROR";
      String message = error != null && error.message() != null ? error.message() : e.getMessage();

      return PgApprovalResult.failure(
          paymentKey, code, truncate(message, MAX_FAILURE_MESSAGE_LENGTH));

    } catch (Exception e) {
      // 타임아웃 등 Toss 응답 자체를 못 받은 경우 -> 조회 API로 재확인
      log.warn(
              "Toss 승인 응답 수신 실패, 결제 조회 API로 상태를 재확인합니다. paymentId={}, paymentKey={}",
              paymentId,
              paymentKey,
              e);
      return checkPaymentStatus(paymentId, paymentKey);
    }
  }

  private PgApprovalResult checkPaymentStatus(Long paymentId, String paymentKey) {
    PgApprovalResult result = tryInquiry(paymentKey);
    if (result != null) {
      return result;
    }

    log.warn("결제 조회 1차 실패, {}ms 후 재시도합니다. paymentKey={}", INQUIRY_RETRY_DELAY_MS, paymentKey);
    try {
      Thread.sleep(INQUIRY_RETRY_DELAY_MS);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }

    result = tryInquiry(paymentKey);
    if (result != null) {
      return result;
    }

    // 재시도까지 실패 -> UNKNOWN으로 저장. 추후 재조회 배치가 찾아낼 수 있도록 함.
    log.error(
            "CRITICAL: Toss 결제 조회마저 실패했습니다. 수동 확인이 필요합니다. paymentId={}, paymentKey={}",
            paymentId,
            paymentKey);

    paymentLogRepository.save(
            PaymentLog.createResponseLog(
                    paymentId,
                    PgProvider.TOSS,
                    paymentKey, // pgTransactionId 자리에 paymentKey 저장. 추후 조회해서 처리할 수 있도록 함.
                    null,
                    null,
                    null,
                    "UNKNOWN",
                    "Toss 승인 응답 및 조회 API 응답을 모두 받지 못했습니다. 수동/배치 재확인이 필요합니다.",
                    0,
                    "UNKNOWN"));

    return PgApprovalResult.unknown(paymentKey, "결제 상태 확인 중 네트워크 오류가 반복되었습니다.");
  }

  /**
   * Toss 조회 API 호출
   * - 성공적으로 상태를 확정할 수 있으면 결과를 반환한다.
   * - 통신 자체가 실패하면 null을 반환해 재시도/최종 UNKNOWN 판단을 호출부에 맡긴다.
   */
  private PgApprovalResult tryInquiry(String paymentKey) {
    try {
      String rawJson =
              restClient
                      .get()
                      .uri(TOSS_PAYMENT_INQUIRY_URL + paymentKey)
                      .header("Authorization", buildAuthorizationHeader())
                      .retrieve()
                      .body(String.class);

      TossConfirmResponse response = objectMapper.readValue(rawJson, TossConfirmResponse.class);

      if ("DONE".equals(response.status())) {
        return PgApprovalResult.success(
                response.paymentKey(),
                response.totalAmount(),
                response.method(),
                response.card() != null ? response.card().number() : null,
                response.card() != null ? response.card().issuerCode() : null);
      }

      return PgApprovalResult.failure(
              paymentKey, "NOT_APPROVED", "결제가 완료되지 않은 상태입니다. status=" + response.status());

    } catch (Exception ex) {
      log.warn("Toss 결제 조회 통신 실패. paymentKey={}", paymentKey, ex);
      return null;
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
