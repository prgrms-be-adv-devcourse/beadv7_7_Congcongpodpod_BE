package kr.lastdish.common.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SqlStatementCounterTests {

  @AfterEach
  void 카운터를_비운다() {
    SqlStatementCounter.clear();
  }

  @Test
  void 계측을_시작하지_않으면_값이_없다() {
    assertThat(SqlStatementCounter.count()).isEmpty();
  }

  @Test
  void 계측을_시작하면_0부터_센다() {
    SqlStatementCounter.start();

    assertThat(SqlStatementCounter.count()).hasValue(0);
  }

  @Test
  void 실행할_때마다_하나씩_증가한다() {
    SqlStatementCounter.start();

    SqlStatementCounter.increment();
    SqlStatementCounter.increment();
    SqlStatementCounter.increment();

    assertThat(SqlStatementCounter.count()).hasValue(3);
  }

  @Test
  void 계측을_시작하지_않았으면_증가시켜도_값이_없다() {
    SqlStatementCounter.increment();

    assertThat(SqlStatementCounter.count()).isEmpty();
  }

  @Test
  void 비우면_값이_없는_상태로_돌아간다() {
    SqlStatementCounter.start();
    SqlStatementCounter.increment();

    SqlStatementCounter.clear();

    assertThat(SqlStatementCounter.count()).isEmpty();
  }

  @Test
  void 다시_시작하면_0부터_다시_센다() {
    SqlStatementCounter.start();
    SqlStatementCounter.increment();

    SqlStatementCounter.start();

    assertThat(SqlStatementCounter.count()).hasValue(0);
  }

  @Test
  void 스레드마다_따로_센다() throws Exception {
    SqlStatementCounter.start();
    SqlStatementCounter.increment();

    OptionalInt 다른_스레드의_값 =
        CompletableFuture.supplyAsync(
                () -> {
                  SqlStatementCounter.start();
                  SqlStatementCounter.increment();
                  SqlStatementCounter.increment();
                  OptionalInt 값 = SqlStatementCounter.count();
                  SqlStatementCounter.clear();
                  return 값;
                })
            .get();

    assertThat(다른_스레드의_값).hasValue(2);
    assertThat(SqlStatementCounter.count()).hasValue(1);
  }
}
