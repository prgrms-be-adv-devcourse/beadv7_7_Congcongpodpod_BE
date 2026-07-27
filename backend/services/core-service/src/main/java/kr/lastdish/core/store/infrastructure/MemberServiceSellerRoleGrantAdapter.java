package kr.lastdish.core.store.infrastructure;

import kr.lastdish.core.store.application.port.out.SellerRoleGrantPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class MemberServiceSellerRoleGrantAdapter implements SellerRoleGrantPort {

  private final RestClient restClient;

  public MemberServiceSellerRoleGrantAdapter(
      @Qualifier("memberServiceRestClient") RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public void grantSellerRole(Long memberId) {
    restClient
        .patch()
        .uri("/internal/v1/members/{memberId}/seller-role", memberId)
        .retrieve()
        .toBodilessEntity();
  }
}
