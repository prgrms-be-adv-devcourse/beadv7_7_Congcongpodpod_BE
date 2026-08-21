package kr.lastdish.ai.infrastructure.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import kr.lastdish.ai.exception.AiErrorCode;
import kr.lastdish.ai.infrastructure.client.dto.InternalStoreResponse;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CoreInternalApiClient {

  private final RestTemplate restTemplate;

  public Optional<InternalStoreResponse> fetchStoreRenewalData(Long storeId) {
    String url = "http://core-service/internal/v1/stores/" + storeId + "/renewal";

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

  public List<InternalStoreResponse> fetchStoresUpdatedWithin(int minutes) {
    String url = "http://core-service/internal/v1/stores/renewal?minutes=" + minutes;

    try {
      ResponseEntity<ApiResponse<List<InternalStoreResponse>>> response =
          restTemplate.exchange(url, HttpMethod.GET, null, new ParameterizedTypeReference<>() {});

      if (response.getBody() != null && response.getBody().data() != null) {
        return response.getBody().data();
      }
      return Collections.emptyList();

    } catch (Exception e) {
      log.error("60초 Polling 중 Core API 호출 실패. minutes={}", minutes, e);
      // Polling 실패 시 다음 주기에 재시도하도록 빈 리스트 반환
      return Collections.emptyList();
    }
  }
}
