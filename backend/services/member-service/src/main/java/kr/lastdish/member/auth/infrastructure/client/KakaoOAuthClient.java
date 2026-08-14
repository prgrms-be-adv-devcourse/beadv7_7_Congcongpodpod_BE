package kr.lastdish.member.auth.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.member.auth.application.dto.KakaoUserInfoResponse;
import kr.lastdish.member.auth.exception.AuthErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
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
      throw new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED);
    }

    try {
      KakaoUserInfoResponse userInfo =
          restClient
              .get()
              .uri("https://kapi.kakao.com/v2/user/me")
              .header("Authorization", "Bearer " + accessToken)
              .retrieve()
              .body(KakaoUserInfoResponse.class);

      if (userInfo == null) {
        throw new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED);
      }

      return userInfo;
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      log.error("[Kakao OAuth] 유저 정보 조회 실패: {}", e.getMessage());
      throw new BusinessException(AuthErrorCode.KAKAO_AUTH_FAILED);
    }
  }

  public void unlink(String socialId) {

    try {
      if (socialId == null || socialId.isBlank()) return;

      MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
      params.add("target_id_type", "user_id");
      params.add("target_id", socialId);

      restClient
          .post()
          .uri("https://kapi.kakao.com/v1/user/unlink")
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .header("Authorization", "KakaoAK " + adminKey)
          .body(params)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception e) {
      log.error("카카오 unlink 처리 중 예외 무시: {}", e.getMessage());
    }
  }

  private record KakaoTokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("token_type") String tokenType,
      @JsonProperty("refresh_token") String refreshToken,
      @JsonProperty("expires_in") Integer expiresIn) {}
}
