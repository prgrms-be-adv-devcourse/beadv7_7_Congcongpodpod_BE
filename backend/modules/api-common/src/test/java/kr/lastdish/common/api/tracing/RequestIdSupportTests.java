package kr.lastdish.common.api.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RequestIdSupportTests {

  @Test
  void null값은_유효하지_않다() {
    assertThat(RequestIdSupport.isValid(null)).isFalse();
  }

  @Test
  void 빈문자열은_유효하지_않다() {
    assertThat(RequestIdSupport.isValid("")).isFalse();
  }

  @Test
  void 영숫자와_점_밑줄_하이픈은_유효하다() {
    assertThat(RequestIdSupport.isValid("abc-123_ABC.1")).isTrue();
  }

  @Test
  void UUID_형식은_유효하다() {
    assertThat(RequestIdSupport.isValid("550e8400-e29b-41d4-a716-446655440000")).isTrue();
  }

  @Test
  void 개행문자가_섞이면_유효하지_않다() {
    assertThat(RequestIdSupport.isValid("abc\ndef")).isFalse();
  }

  @Test
  void 공백이_섞이면_유효하지_않다() {
    assertThat(RequestIdSupport.isValid("abc def")).isFalse();
  }

  @Test
  void 예순네글자는_유효하다() {
    assertThat(RequestIdSupport.isValid("a".repeat(64))).isTrue();
  }

  @Test
  void 예순다섯글자는_유효하지_않다() {
    assertThat(RequestIdSupport.isValid("a".repeat(65))).isFalse();
  }

  @Test
  void UNKNOWN은_검증규칙을_통과한다() {
    // 로그 대체값도 같은 형식 규칙 안에 있어야 로그 파싱이 일관된다.
    assertThat(RequestIdSupport.isValid(RequestIdSupport.UNKNOWN)).isTrue();
  }
}
