package kr.lastdish.member.auth.infrastructure.client;

import kr.lastdish.member.auth.application.dto.KakaoUserInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOAuthClient {

  private final RestClient restClient = RestClient.create();

  @Value("${kakao.oauth.client-id}")
  private String clientId;

  @Value("${kakao.oauth.redirect-uri}")
  private String redirectUri;

  @Value("${kakao.oauth.client-secret:}")
  private String clientSecret;

  @Value("${kakao.oauth.admin-key}")
  private String adminKey;

  public String getAccessToken(String code) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", clientId);
    params.add("redirect_uri", redirectUri);
    params.add("code", code);
    if (clientSecret != null && !clientSecret.isBlank()) {
      params.add("client_secret", clientSecret);
    }

    KakaoTokenResponse response =
        restClient
            .post()
            .uri("https://kauth.kakao.com/oauth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(params)
            .retrieve()
            .body(KakaoTokenResponse.class);

    return response != null ? response.accessToken() : null;
  }

  public KakaoUserInfoResponse getKakaoUserInfo(String code) {
    String accessToken = getAccessToken(code);
    if (accessToken == null) {
      throw new RuntimeException("카카오 Access Token 발급 실패");
    }

    return restClient
        .get()
        .uri("https://kapi.kakao.com/v2/user/me")
        .header("Authorization", "Bearer " + accessToken)
        .retrieve()
        .body(KakaoUserInfoResponse.class);
  }

  public void unlink(String socialId) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("target_id_type", "user_id");
    params.add("target_id", socialId);

    try {
      restClient
          .post()
          .uri("https://kapi.kakao.com/v1/user/unlink")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .header("Authorization", "KakaoAK " + adminKey) // Admin Key 인증 방식
          .body(params)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      throw new RuntimeException("카카오 연결 끊기 실패: " + e.getMessage(), e);
    }
  }

  private record KakaoTokenResponse(
      String accessToken, String tokenType, String refreshToken, Integer expiresIn) {}
}
