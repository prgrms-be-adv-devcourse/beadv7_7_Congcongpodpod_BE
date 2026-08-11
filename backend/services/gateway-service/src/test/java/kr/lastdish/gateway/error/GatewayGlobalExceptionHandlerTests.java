package kr.lastdish.gateway.error;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class GatewayGlobalExceptionHandlerTests {

  private ObjectMapper objectMapper;
  private GatewayGlobalExceptionHandler handler;
  private ch.qos.logback.classic.Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    handler = new GatewayGlobalExceptionHandler(objectMapper);

    logger =
        (ch.qos.logback.classic.Logger)
            LoggerFactory.getLogger(GatewayGlobalExceptionHandler.class);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
  }

  @AfterEach
  void detachAppender() {
    logger.detachAppender(appender);
    appender.stop();
  }

  @Test
  void mapsResponseStatusesToGatewayErrors() throws Exception {
    assertError(
        new ResponseStatusException(HttpStatus.BAD_REQUEST, "test"),
        HttpStatus.BAD_REQUEST,
        GatewayErrorCode.INVALID_REQUEST);
    assertError(
        new ResponseStatusException(HttpStatus.NOT_FOUND, "test"),
        HttpStatus.NOT_FOUND,
        GatewayErrorCode.ROUTE_NOT_FOUND);
    assertError(
        new ResponseStatusException(HttpStatus.BAD_GATEWAY, "test"),
        HttpStatus.BAD_GATEWAY,
        GatewayErrorCode.BAD_GATEWAY);
    assertError(
        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "test"),
        HttpStatus.SERVICE_UNAVAILABLE,
        GatewayErrorCode.SERVICE_UNAVAILABLE);
    assertError(
        new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "test"),
        HttpStatus.GATEWAY_TIMEOUT,
        GatewayErrorCode.GATEWAY_TIMEOUT);
  }

  @Test
  void mapsNestedConnectionFailureToServiceUnavailable() throws Exception {
    assertError(
        new IllegalStateException(new ConnectException("connection refused")),
        HttpStatus.SERVICE_UNAVAILABLE,
        GatewayErrorCode.SERVICE_UNAVAILABLE);
    assertError(
        new IllegalStateException(new UnknownHostException("core-service")),
        HttpStatus.SERVICE_UNAVAILABLE,
        GatewayErrorCode.SERVICE_UNAVAILABLE);
    assertError(
        new IllegalStateException(new NoRouteToHostException("host is unreachable")),
        HttpStatus.SERVICE_UNAVAILABLE,
        GatewayErrorCode.SERVICE_UNAVAILABLE);
  }

  @Test
  void mapsNestedTimeoutToGatewayTimeout() throws Exception {
    assertError(
        new IllegalStateException(new TimeoutException("response timeout")),
        HttpStatus.GATEWAY_TIMEOUT,
        GatewayErrorCode.GATEWAY_TIMEOUT);
  }

  @Test
  void mapsUnexpectedExceptionToInternalError() throws Exception {
    assertError(
        new IllegalStateException("unexpected"),
        HttpStatus.INTERNAL_SERVER_ERROR,
        GatewayErrorCode.INTERNAL_ERROR);
  }

  @Test
  void propagatesExceptionWhenResponseIsAlreadyCommitted() {
    MockServerWebExchange exchange = exchange();
    exchange.getResponse().setComplete().block();
    IllegalStateException exception = new IllegalStateException("committed");

    StepVerifier.create(handler.handle(exchange, exception))
        .expectErrorMatches(error -> error == exception)
        .verify();
  }

  @Test
  @DisplayName("하위 서비스 timeout은 method·path·오류 코드와 함께 WARN으로 기록한다")
  void logsGatewayTimeoutAsWarn() {
    handler
        .handle(exchange(), new IllegalStateException(new TimeoutException("response timeout")))
        .block();

    assertThat(appender.list).hasSize(1);

    ILoggingEvent event = appender.list.getFirst();
    assertThat(event.getLevel()).isEqualTo(Level.WARN);
    assertThat(event.getFormattedMessage())
        .contains("GET")
        .contains("/test")
        .contains(GatewayErrorCode.GATEWAY_TIMEOUT.getCode())
        .contains(TimeoutException.class.getName());
  }

  @Test
  @DisplayName("하위 서비스 연결 실패는 WARN으로 기록한다")
  void logsServiceUnavailableAsWarn() {
    handler
        .handle(exchange(), new IllegalStateException(new ConnectException("connection refused")))
        .block();

    assertThat(appender.list).hasSize(1);

    ILoggingEvent event = appender.list.getFirst();
    assertThat(event.getLevel()).isEqualTo(Level.WARN);
    assertThat(event.getFormattedMessage())
        .contains(GatewayErrorCode.SERVICE_UNAVAILABLE.getCode())
        .contains(ConnectException.class.getName());
  }

  @Test
  @DisplayName("Gateway 내부 오류는 예외 스택과 함께 ERROR로 기록한다")
  void logsInternalErrorWithStackTrace() {
    handler.handle(exchange(), new IllegalStateException("unexpected")).block();

    assertThat(appender.list).hasSize(1);

    ILoggingEvent event = appender.list.getFirst();
    assertThat(event.getLevel()).isEqualTo(Level.ERROR);
    assertThat(event.getThrowableProxy()).isNotNull();
    assertThat(event.getFormattedMessage())
        .contains(GatewayErrorCode.INTERNAL_ERROR.getCode())
        .contains(IllegalStateException.class.getName());
  }

  @Test
  @DisplayName("일반적인 4xx는 로그를 남기지 않는다")
  void doesNotLogClientErrors() {
    handler.handle(exchange(), new ResponseStatusException(HttpStatus.NOT_FOUND, "test")).block();

    assertThat(appender.list).isEmpty();
  }

  private void assertError(
      Throwable exception, HttpStatus expectedStatus, GatewayErrorCode expectedError)
      throws Exception {
    MockServerWebExchange exchange = exchange();

    handler.handle(exchange, exception).block();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(expectedStatus);

    String body = exchange.getResponse().getBodyAsString().block();
    JsonNode json = objectMapper.readTree(body);

    assertThat(json.get("success").asBoolean()).isFalse();
    assertThat(json.get("data")).isNull();
    assertThat(json.get("error").get("code").asText()).isEqualTo(expectedError.getCode());
    assertThat(json.get("error").get("message").asText()).isEqualTo(expectedError.getMessage());
    assertThat(json.get("timestamp").asText()).isNotBlank();
  }

  private MockServerWebExchange exchange() {
    return MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
  }
}
