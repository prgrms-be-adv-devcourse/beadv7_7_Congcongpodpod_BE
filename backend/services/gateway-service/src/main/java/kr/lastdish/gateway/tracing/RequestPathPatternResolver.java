package kr.lastdish.gateway.tracing;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * 로그에 남겨도 안전한 경로 패턴을 결정한다.
 *
 * <p>실제 요청 경로에는 회원·주문·매장 식별자가 들어 있으므로 로그에 그대로 남기지 않는다. 대신 라우팅이 매칭한 패턴만 사용하고, 신뢰할 수 없으면 실제 경로로 대체하지
 * 않고 {@link #UNMATCHED}를 남긴다.
 *
 * <p>Gateway는 라우트 후보를 순서대로 검사하므로, 앞선 후보가 Path 조건만 통과하고 다른 조건에서 탈락해도 그 후보의 패턴이 attribute에 남을 수 있다.
 * 따라서 최종 선택된 라우트 ID와 패턴을 남긴 라우트 ID가 같을 때만 신뢰한다.
 *
 * <p>완료 로그와 예외 로그가 같은 판단을 쓰도록 두 곳이 이 클래스를 함께 사용한다.
 */
public final class RequestPathPatternResolver {

  /** 경로 패턴을 신뢰할 수 없을 때 남기는 값. */
  public static final String UNMATCHED = "unmatched";

  private RequestPathPatternResolver() {}

  public static String resolve(ServerWebExchange exchange) {
    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    String matchedPath =
        exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ATTR);
    String matchedRouteId =
        exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_MATCHED_PATH_ROUTE_ID_ATTR);

    if (route == null || matchedPath == null || !route.getId().equals(matchedRouteId)) {
      return UNMATCHED;
    }

    return matchedPath;
  }
}
