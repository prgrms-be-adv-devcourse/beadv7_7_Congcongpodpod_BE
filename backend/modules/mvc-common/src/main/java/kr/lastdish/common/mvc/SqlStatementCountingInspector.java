package kr.lastdish.common.mvc;

import org.hibernate.resource.jdbc.spi.StatementInspector;

/**
 * Hibernate가 SQL을 실행하기 직전에 거쳐 가는 지점에서 실행 수만 세고 문장은 그대로 통과시킨다.
 *
 * <p>{@link StatementInspector}는 SQL을 고쳐 쓰라고 열어 둔 자리지만 여기서는 세기만 한다. 별도의 프록시 DataSource나 에이전트를 붙이지
 * 않고 이미 들어와 있는 Hibernate만으로 계측할 수 있어서, 운영 구성을 건드리지 않고 껐다 켤 수 있다.
 *
 * <p>세는 값은 {@link SqlStatementCounter}가 스레드별로 들고 있고, 요청 경계에서 {@link
 * RequestCompletionLoggingFilter}가 시작·정리한다. 그래서 요청 밖에서 도는 스케줄러나 이벤트 소비자의 SQL은 세지 않는다 — 시작되지 않은
 * 스레드에서는 {@link SqlStatementCounter#increment()}가 아무 일도 하지 않기 때문이다.
 */
public class SqlStatementCountingInspector implements StatementInspector {

  @Override
  public String inspect(String sql) {
    SqlStatementCounter.increment();
    return sql;
  }
}
