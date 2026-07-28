package kr.lastdish.core.order.infrastructure;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.order.application.dto.OrderMemberInfo;
import kr.lastdish.core.order.application.port.out.OrderMemberQueryPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class MemberServiceOrderMemberAdapter implements OrderMemberQueryPort {

  private final RestClient restClient;

  public MemberServiceOrderMemberAdapter(
      @Qualifier("memberServiceRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public OrderMemberInfo getOrderMemberInfo(Long memberId) {
    try {
      ApiResponse<MemberServiceOrderMemberResponse> response =
          restClient
              .get()
              .uri("/internal/v1/members/{memberId}/order-info", memberId)
              .retrieve()
              .body(new ParameterizedTypeReference<>() {});

      MemberServiceOrderMemberResponse data = getValidData(response);
      return new OrderMemberInfo(data.name(), data.phone());
    } catch (HttpClientErrorException.NotFound exception) {
      throw new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "주문 회원을 찾을 수 없습니다.");
    } catch (RestClientException exception) {
      throw memberServiceUnavailable();
    }
  }

  private MemberServiceOrderMemberResponse getValidData(
      ApiResponse<MemberServiceOrderMemberResponse> response) {
    if (response == null || !response.success() || response.data() == null) {
      throw memberServiceUnavailable();
    }

    MemberServiceOrderMemberResponse data = response.data();
    if (isBlank(data.name()) || isBlank(data.phone())) {
      throw memberServiceUnavailable();
    }
    return data;
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private BusinessException memberServiceUnavailable() {
    return new BusinessException(
        CommonErrorCode.SERVICE_UNAVAILABLE, "회원 정보를 조회할 수 없습니다. 잠시 후 다시 시도해 주세요.");
  }

  private record MemberServiceOrderMemberResponse(String name, String phone) {}
}
