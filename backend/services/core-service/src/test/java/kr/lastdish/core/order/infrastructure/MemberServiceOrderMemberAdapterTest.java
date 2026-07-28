package kr.lastdish.core.order.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.order.application.dto.OrderMemberInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class MemberServiceOrderMemberAdapterTest {

  private final RestClient.Builder restClientBuilder =
      RestClient.builder().baseUrl("http://member-service:8080");
  private final MockRestServiceServer server =
      MockRestServiceServer.bindTo(restClientBuilder).build();
  private final MemberServiceOrderMemberAdapter adapter =
      new MemberServiceOrderMemberAdapter(restClientBuilder.build());

  @Test
  void getOrderMemberInfo_success() {
    server
        .expect(requestTo("http://member-service:8080/internal/v1/members/1/order-info"))
        .andRespond(
            withSuccess(
                """
                {
                  "success": true,
                  "data": {
                    "name": "김나영",
                    "phone": "010-1234-5678"
                  }
                }
                """,
                MediaType.APPLICATION_JSON));

    OrderMemberInfo result = adapter.getOrderMemberInfo(1L);

    assertThat(result.name()).isEqualTo("김나영");
    assertThat(result.phone()).isEqualTo("010-1234-5678");
    server.verify();
  }

  @Test
  void getOrderMemberInfo_nullData_throwsApplicationException() {
    server
        .expect(requestTo("http://member-service:8080/internal/v1/members/1/order-info"))
        .andRespond(withSuccess("{\"success\": true, \"data\": null}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> adapter.getOrderMemberInfo(1L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            exception ->
                assertThat(((BusinessException) exception).getErrorCode())
                    .isEqualTo(CommonErrorCode.SERVICE_UNAVAILABLE));
  }

  @Test
  void getOrderMemberInfo_blankRequiredField_throwsServiceUnavailable() {
    server
        .expect(requestTo("http://member-service:8080/internal/v1/members/1/order-info"))
        .andRespond(
            withSuccess(
                """
                {
                  "success": true,
                  "data": {
                    "name": " ",
                    "phone": "010-1234-5678"
                  }
                }
                """,
                MediaType.APPLICATION_JSON));

    assertErrorCode(() -> adapter.getOrderMemberInfo(1L), CommonErrorCode.SERVICE_UNAVAILABLE);
  }

  @Test
  void getOrderMemberInfo_memberNotFound_throwsEntityNotFound() {
    server
        .expect(requestTo("http://member-service:8080/internal/v1/members/1/order-info"))
        .andRespond(withResourceNotFound());

    assertErrorCode(() -> adapter.getOrderMemberInfo(1L), CommonErrorCode.ENTITY_NOT_FOUND);
  }

  @Test
  void getOrderMemberInfo_communicationFailure_throwsApplicationException() {
    server
        .expect(requestTo("http://member-service:8080/internal/v1/members/1/order-info"))
        .andRespond(withServerError());

    assertThatThrownBy(() -> adapter.getOrderMemberInfo(1L))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            exception ->
                assertThat(((BusinessException) exception).getErrorCode())
                    .isEqualTo(CommonErrorCode.SERVICE_UNAVAILABLE));
  }

  private void assertErrorCode(Runnable action, CommonErrorCode expectedErrorCode) {
    assertThatThrownBy(action::run)
        .isInstanceOf(BusinessException.class)
        .satisfies(
            exception ->
                assertThat(((BusinessException) exception).getErrorCode())
                    .isEqualTo(expectedErrorCode));
  }
}
