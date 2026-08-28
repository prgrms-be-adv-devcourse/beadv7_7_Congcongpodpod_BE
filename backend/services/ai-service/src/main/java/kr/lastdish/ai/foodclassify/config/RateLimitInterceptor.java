package kr.lastdish.ai.foodclassify.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kr.lastdish.ai.foodclassify.exception.AiErrorCode;
import kr.lastdish.common.api.exception.BusinessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** IP별 API 요청 수 제한 인터셉터 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  // IP당 제한 규칙: 1분당 최대 3회 요청 허용
  private Bucket createNewBucket() {
    Bandwidth limit =
        Bandwidth.builder().capacity(3).refillIntervally(3, Duration.ofMinutes(1)).build();
    return Bucket.builder().addLimit(limit).build();
  }

  @Override
  public boolean preHandle(
      @Nonnull HttpServletRequest request,
      @Nonnull HttpServletResponse response,
      @Nonnull Object handler) {
    String clientIp = getClientIp(request);
    Bucket bucket = buckets.computeIfAbsent(clientIp, k -> createNewBucket());

    if (!bucket.tryConsume(1)) {
      throw new BusinessException(AiErrorCode.TOO_MANY_REQUESTS);
    }

    return true;
  }

  // 1시간마다 토큰이 꽉 찬(더 이상 제한 대상이 아닌) 버킷 정리
  @Scheduled(fixedRate = 3600000)
  public void cleanUpBuckets() {
    buckets
        .entrySet()
        .removeIf(
            entry -> {
              // 버킷의 가용 토큰 수가 최대 용량(3개)과 같으면 지움
              return entry.getValue().getAvailableTokens() >= 3;
            });
  }

  // 프록시 환경 고려 클라이언트 IP 추출
  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    return ip != null ? ip.split(",")[0].trim() : "unknown";
  }
}
