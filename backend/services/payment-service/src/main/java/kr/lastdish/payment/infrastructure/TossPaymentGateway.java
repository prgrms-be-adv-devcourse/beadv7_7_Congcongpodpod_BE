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
  // paymentKey로 조회 - confirm 응답 실패 직후 즉시 재확인
  private static final String TOSS_PAYMENT_KEY_STATUS_URL =
      "https://api.tosspayments.com/v1/payments/";
  // orderId로 조회 - PROCESSING 상태로 남음 재조회 배치
  private static final String TOSS_ORDER_STATUS_URL =
      "https://api.tosspayments.com/v1/payments/orders/";
  private static final int MAX_FAILURE_MESSAGE_LENGTH = 500;
  private static final long STATUS_CHECK_RETRY_DELAY_MS = 700L;

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
      // 타임아웃 등 Toss 응답 자체를 못 받은 경우 -> paymentKey 조회 API로 재확인
      log.warn(
          "Toss 승인 응답 수신 실패, 결제 조회 API로 상태를 재확인합니다. paymentId={}, paymentKey={}",
          paymentId,
          paymentKey,
          e);
      return checkPaymentStatusByPaymentKey(paymentId, paymentKey);
    }
  }

  private PgApprovalResult checkPaymentStatusByPaymentKey(Long paymentId, String paymentKey) {
    PgApprovalResult result = tryCheckStatusByPaymentKey(paymentKey);
    if (result != null) {
      return result;
    }

    log.warn("결제 조회 1차 실패, {}ms 후 재시도합니다. paymentKey={}", STATUS_CHECK_RETRY_DELAY_MS, paymentKey);
    try {
      Thread.sleep(STATUS_CHECK_RETRY_DELAY_MS);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }

    result = tryCheckStatusByPaymentKey(paymentKey);
    if (result != null) {
      return result;
    }

    // 재시도까지 실패 시 UNKNOWN으로 저장
    log.error(
        "Toss 결제 조회마저 실패했습니다. 수동 확인이 필요합니다. paymentId={}, paymentKey={}", paymentId, paymentKey);

    paymentLogRepository.save(
        PaymentLog.createResponseLog(
            paymentId,
            PgProvider.TOSS,
            paymentKey, // pgTransactionId 자리에 paymentKey 저장. 추후 조회해서 처리할 수 있도록 함.
            null,
            null,
            null,
            "UNKNOWN",
            "Toss 승인 응답 및 조회 API 응답을 모두 받지 못했습니다. 재확인이 필요합니다.",
            0,
            "UNKNOWN"));

    return PgApprovalResult.unknown(paymentKey, "결제 상태 확인 중 네트워크 오류가 반복되었습니다.");
  }

  // paymentKey로 Toss 조회 API 호출 후 최종 상태 확정
  private PgApprovalResult tryCheckStatusByPaymentKey(String paymentKey) {
    try {
      String rawJson =
          restClient
              .get()
              .uri(TOSS_PAYMENT_KEY_STATUS_URL + paymentKey)
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

  // orderId로 Toss 조회 API 호출 - PROCESSING로 남은 건 재조회
  @Override
  public PgApprovalResult checkStatus(String merchantOrderId) {
    try {
      String rawJson =
          restClient
              .get()
              .uri(TOSS_ORDER_STATUS_URL + merchantOrderId)
              .header("Authorization", buildAuthorizationHeader())
              .retrieve()
              .body(String.class);

      TossConfirmResponse response = objectMapper.readValue(rawJson, TossConfirmResponse.class);

      if (!"DONE".equals(response.status())) {
        log.warn(
            "Toss 재조회 응답이 200인데 status가 DONE이 아닙니다. status={}, merchantOrderId={}",
            response.status(),
            merchantOrderId);
        return PgApprovalResult.unknown(
            response.paymentKey(), "예상치 못한 status 값입니다. status=" + response.status());
      }

      return PgApprovalResult.success(
          response.paymentKey(),
          response.totalAmount(),
          response.method(),
          response.card() != null ? response.card().number() : null,
          response.card() != null ? response.card().issuerCode() : null);

    } catch (RestClientResponseException e) {
      return classifyStatusErrorResponse(merchantOrderId, e);

    } catch (Exception e) {
      log.warn("Toss 재조회 API 통신 실패. merchantOrderId={}", merchantOrderId, e);
      return PgApprovalResult.unknown(null, "재조회 중 네트워크 오류가 발생했습니다.");
    }
  }

  // 404(NOT_FOUND_PAYMENT)는 threshold(40분) 경과 시 최종 실패 확정
  private PgApprovalResult classifyStatusErrorResponse(
      String merchantOrderId, RestClientResponseException e) {
    int statusCode = e.getStatusCode().value();

    if (statusCode == 404) {
      log.info("재조회 결과 404 - threshold 경과 시 최종 미승인으로 확정. merchantOrderId={}", merchantOrderId);
      return PgApprovalResult.failure(
          null, "NOT_FOUND_AFTER_THRESHOLD", "재조회 threshold 경과 후에도 조회되지 않아 최종 실패로 확정합니다.");
    }

    if (statusCode == 429 || statusCode >= 500) {
      log.warn("Toss 재조회 API 일시 장애(status={}). merchantOrderId={}", statusCode, merchantOrderId);
      return PgApprovalResult.unknown(null, "Toss 재조회 API 일시 장애. status=" + statusCode);
    }

    TossErrorResponse error = e.getResponseBodyAs(TossErrorResponse.class);
    String message = error != null && error.message() != null ? error.message() : e.getMessage();
    log.warn(
        "Toss 재조회 API 예상치 못한 오류 응답. merchantOrderId={}, status={}", merchantOrderId, statusCode);
    return PgApprovalResult.unknown(
        null, truncate("재조회 API 오류: " + message, MAX_FAILURE_MESSAGE_LENGTH));
  }
}
