package kr.lastdish.core.geocoding.infrastructure;

import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.geocoding.presentation.dto.GeocodingAddressResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class NaverGeocodingClient {

  private static final String BASE_URL = "https://maps.apigw.ntruss.com";

  private final RestClient restClient;
  private final String clientId;
  private final String clientSecret;

  public NaverGeocodingClient(
      RestClient.Builder restClientBuilder,
      @Value("${naver.maps.client-id:${NAVER_MAP_CLIENT_ID:}}") String clientId,
      @Value("${naver.maps.client-secret:${NAVER_MAP_CLIENT_SECRET:}}") String clientSecret) {
    this.restClient = restClientBuilder.baseUrl(BASE_URL).build();
    this.clientId = clientId;
    this.clientSecret = clientSecret;
  }

  public List<GeocodingAddressResponse> search(String query) {
    validateConfiguration();

    try {
      NaverGeocodingResponse response =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/map-geocode/v2/geocode")
                          .queryParam("query", query)
                          .queryParam("count", 10)
                          .build())
              .header("x-ncp-apigw-api-key-id", clientId)
              .header("x-ncp-apigw-api-key", clientSecret)
              .accept(MediaType.APPLICATION_JSON)
              .retrieve()
              .body(NaverGeocodingResponse.class);

      if (response == null || response.addresses() == null) {
        throw new BusinessException(ErrorCode.GEOCODING_SERVICE_ERROR);
      }

      return response.addresses().stream().map(NaverAddress::toResponse).toList();
    } catch (BusinessException exception) {
      throw exception;
    } catch (RestClientException | IllegalArgumentException exception) {
      log.warn("네이버 주소 검색 요청에 실패했습니다. queryLength={}", query.length(), exception);
      throw new BusinessException(ErrorCode.GEOCODING_SERVICE_ERROR);
    }
  }

  private void validateConfiguration() {
    if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
      throw new BusinessException(ErrorCode.GEOCODING_NOT_CONFIGURED);
    }
  }

  record NaverGeocodingResponse(List<NaverAddress> addresses) {}

  record NaverAddress(
      String roadAddress, String jibunAddress, String englishAddress, String x, String y) {

    GeocodingAddressResponse toResponse() {
      return new GeocodingAddressResponse(
          roadAddress, jibunAddress, englishAddress, new BigDecimal(y), new BigDecimal(x));
    }
  }
}
