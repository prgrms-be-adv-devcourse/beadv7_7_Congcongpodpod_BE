package kr.lastdish.ai.elastic.infrastructure.client;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import kr.lastdish.ai.elastic.infrastructure.client.dto.InternalStoreResponse;
import kr.lastdish.ai.foodclassify.exception.AiErrorCode;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreInternalApiClient {

  private final RestTemplate restTemplate;

  @Value("${services.core.base-url}")
  private String coreBaseUrl;

  public Optional<InternalStoreResponse> fetchStoreRenewalData(Long storeId) {
    String url = coreBaseUrl + "/internal/v1/stores/" + storeId + "/renewal";
    try {
      ResponseEntity<ApiResponse<InternalStoreResponse>> response =
          restTemplate.exchange(
              url,
              HttpMethod.GET,
              null,
              new ParameterizedTypeReference<ApiResponse<InternalStoreResponse>>() {});

      if (response.getBody() != null && response.getBody().data() != null) {
        return Optional.of(response.getBody().data());
      }
      return Optional.empty();

    } catch (HttpClientErrorException.NotFound e) {
      // 404 Not Found: Core 서비스에 해당 매장이 없거나 삭제된 경우
      log.warn("Core 서비스에 매장이 존재하지 않습니다 (404). storeId={}", storeId);
      return Optional.empty();

    } catch (HttpServerErrorException | ResourceAccessException e) {
      // 5xx 서버 에러 또는 타임아웃, 통신 장애 -> 예외를 던져 Kafka Retry
      log.error("Core 서비스 통신 장애 발생. storeId={}, error={}", storeId, e.getMessage());
      throw new BusinessException(AiErrorCode.CORE_API_COMMUNICATION_ERROR);

    } catch (Exception e) {
      log.error("Renewal API 호출 중 예상치 못한 에러 발생. storeId={}", storeId, e);
      throw new BusinessException(AiErrorCode.CORE_API_COMMUNICATION_ERROR);
    }
  }

  public List<InternalStoreResponse> fetchStoresUpdatedWithin(Instant from, Instant to) {
    String url =
        UriComponentsBuilder.fromUriString(coreBaseUrl + "/internal/v1/stores/renewal")
            .queryParam("from", from)
            .queryParam("to", to)
            .toUriString();

    try {
      ResponseEntity<ApiResponse<List<InternalStoreResponse>>> response =
          restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

      if (response.getBody() != null && response.getBody().data() != null) {
        return response.getBody().data();
      }
      return List.of();

      // 4xx 예외 - 통신 장애 예외를 샄며버리면 watermark가 멈추거나 원인을 찾기 어려워짐
    } catch (HttpClientErrorException e) {
      log.error(
          "Polling 중 Core API 클라이언트 에러 발생 (4xx). from={}, to={}, status={}",
          from,
          to,
          e.getStatusCode());
      return List.of();

    } catch (HttpServerErrorException | ResourceAccessException e) {
      // 5xx 서버 에러 또는 타임아웃, 통신 장애 -> 예외를 던져서 watermark가 전진하지 않도록 함
      log.error("Polling 중 Core API 통신 장애 발생. from={}, to={}, error={}", from, to, e.getMessage());
      throw new BusinessException(AiErrorCode.CORE_API_COMMUNICATION_ERROR);

    } catch (Exception e) {
      log.error("Polling 중 Core API 호출 실패. from={}, to={}", from, to, e);
      throw new BusinessException(AiErrorCode.CORE_API_COMMUNICATION_ERROR);
    }
  }
}
