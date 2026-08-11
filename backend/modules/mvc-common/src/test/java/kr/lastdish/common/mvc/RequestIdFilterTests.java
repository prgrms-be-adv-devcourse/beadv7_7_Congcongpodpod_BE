package kr.lastdish.common.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.regex.Pattern;
import kr.lastdish.common.api.tracing.RequestIdSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTests {

  private static final Pattern UUID_FORMAT =
      Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");

  private final RequestIdFilter filter = new RequestIdFilter();

  @Test
  void 헤더의_번호를_처리중_MDC에_올린다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdSupport.HEADER_NAME, "edge-abc-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    String[] observedDuringChain = new String[1];
    MockFilterChain chain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            observedDuringChain[0] = MDC.get(RequestIdSupport.KEY);
          }
        };

    filter.doFilter(request, response, chain);

    assertThat(observedDuringChain[0]).isEqualTo("edge-abc-123");
  }

  @Test
  void 요청이_끝나면_MDC를_정리한다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdSupport.HEADER_NAME, "edge-abc-123");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertThat(MDC.get(RequestIdSupport.KEY)).isNull();
  }

  @Test
  void 헤더가_없으면_자체발급해_MDC에_올린다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    String[] observedDuringChain = new String[1];
    MockFilterChain chain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            observedDuringChain[0] = MDC.get(RequestIdSupport.KEY);
          }
        };

    filter.doFilter(request, response, chain);

    assertThat(observedDuringChain[0]).matches(UUID_FORMAT);
  }

  @Test
  void 형식이_어긋난_헤더는_버리고_자체발급한다() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(RequestIdSupport.HEADER_NAME, "abc def");
    MockHttpServletResponse response = new MockHttpServletResponse();

    String[] observedDuringChain = new String[1];
    MockFilterChain chain =
        new MockFilterChain() {
          @Override
          public void doFilter(
              jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
            observedDuringChain[0] = MDC.get(RequestIdSupport.KEY);
          }
        };

    filter.doFilter(request, response, chain);

    assertThat(observedDuringChain[0]).matches(UUID_FORMAT);
  }
}
