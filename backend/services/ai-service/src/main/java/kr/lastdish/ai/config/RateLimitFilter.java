package kr.lastdish.ai.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kr.lastdish.ai.exception.AiErrorCode;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

//  Spring WebFlux 기반의 요청 수 제한 필터
//  특정 API 경로에 대해 IP별로 분당 요청 횟수를 제한

@Component
public class RateLimitFilter implements WebFilter {

  // IP별 버킷 저장소
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  /**
   * IP별로 새로 부여할 Bucket 객체를 생성하는 메서드 - capacity(3): 순간적으로 가질 수 있는 최대 토큰 수 (최대 3회 연속 요청) -
   * refillIntervally(3, Duration.ofMinutes(1)): 1분마다 3개의 토큰을 새로 충전
   */
  private Bucket createNewBucket() {
    Bandwidth limit =
        Bandwidth.builder().capacity(3).refillIntervally(3, Duration.ofMinutes(1)).build();
    return Bucket.builder().addLimit(limit).build();
  }

  @Override
  @Nonnull
  public Mono<Void> filter(@Nonnull ServerWebExchange exchange, @Nonnull WebFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();

    // 1. 트래픽 제한 대상 API 경로 검사
    if ("/api/v1/ai/classify".equals(path)) {
      String clientIp = getClientIp(exchange);

      // 해당 IP의 버킷 조회 (만약 첫 방문 IP라면 createNewBucket으로 새 버킷 생성 및 저장)
      Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createNewBucket());

      // 2. 토큰 1개 차감 시도
      if (!bucket.tryConsume(1)) {
        AiErrorCode errorCode = AiErrorCode.TOO_MANY_REQUESTS;

        exchange.getResponse().setStatusCode(errorCode.getStatus());
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Enum에 정의된 code("AI002")와 message를 활용해 표준 JSON 바디 생성
        String jsonResponse =
            String.format(
                "{\"code\":\"%s\",\"message\":\"%s\"}",
                errorCode.getCode(), errorCode.getMessage());

        // JSON 문자열을 UTF-8 바이트 배열로 변환 후 WebFlux DataBuffer로 래핑
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);

        // 컨트롤러/다음 필터로 넘기지 않고 바로 에러 응답을 클라이언트에 반환 후 종료
        return exchange.getResponse().writeWith(Mono.just(buffer));
      }
    }

    // 3. 제한 대상 경로가 아니거나 토큰이 정상 차감된 경우 다음 필터/컨트롤러로 진행
    return chain.filter(exchange);
  }

  /** 로드밸런서를 거쳐 들어오는 클라이언트의 실제 IP 추출 메서드 */
  private String getClientIp(ServerWebExchange exchange) {
    // 프록시 환경에서 전달되는 X-Forwarded-For 헤더값 확인
    String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

    // 헤더가 없다면 직접 연결된 RemoteAddress에서 IP 추출
    if (ip == null || ip.isEmpty()) {
      ip =
          exchange.getRequest().getRemoteAddress() != null
              ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
              : "unknown";
    }

    // X-Forwarded-For에 여러 IP가 콤마(,)로 나열된 경우 맨 앞의 최초 클라이언트 IP 선택
    return ip.split(",")[0].trim();
  }
}
