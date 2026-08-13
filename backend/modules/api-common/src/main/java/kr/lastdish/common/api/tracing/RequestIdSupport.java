package kr.lastdish.common.api.tracing;

import java.util.regex.Pattern;

/**
 * Gateway(WebFlux)와 하위 서비스(서블릿) 양쪽이 requestId의 헤더 이름·검증 규칙·로그 필드 이름을 동일하게 쓰기 위한 공유 상수·규칙.
 *
 * <p>{@link #KEY}는 Gateway의 Reactor exchange attribute 키, 하위 서비스의 MDC 키, 로그에 남기는 필드 이름으로 모두 재사용한다.
 * 이름이 갈리면 로그 검색이 어긋나므로 하나로 유지한다.
 */
public final class RequestIdSupport {

  public static final String HEADER_NAME = "X-Request-Id";
  public static final String KEY = "requestId";

  /** 요청 번호를 확정하기 전 단계에서 예외가 난 경우처럼, 번호를 알 수 없을 때 로그에 남기는 값. */
  public static final String UNKNOWN = "unknown";

  private static final Pattern VALID_FORMAT = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");

  private RequestIdSupport() {}

  public static boolean isValid(String value) {
    return value != null && VALID_FORMAT.matcher(value).matches();
  }
}
