package kr.lastdish.common.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SqlStatementCountingInspectorTests {

  private final SqlStatementCountingInspector inspector = new SqlStatementCountingInspector();

  @AfterEach
  void 카운터를_비운다() {
    SqlStatementCounter.clear();
  }

  @Test
  void 검사한_SQL을_바꾸지_않고_그대로_돌려준다() {
    String sql = "select d1_0.id from dishes d1_0 where d1_0.store_id=?";

    assertThat(inspector.inspect(sql)).isEqualTo(sql);
  }

  @Test
  void 검사할_때마다_실행_수를_하나씩_올린다() {
    SqlStatementCounter.start();

    inspector.inspect("select 1");
    inspector.inspect("select 2");

    assertThat(SqlStatementCounter.count()).hasValue(2);
  }

  @Test
  void 계측_중이_아니면_세지_않는다() {
    inspector.inspect("select 1");

    assertThat(SqlStatementCounter.count()).isEmpty();
  }
}
