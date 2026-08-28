package kr.lastdish.common.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

class MvcCommonAutoConfigurationTests {

  private final WebApplicationContextRunner runner =
      new WebApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(MvcCommonAutoConfiguration.class));

  @Test
  void 기본값에서는_SQL_계측을_등록하지_않는다() {
    runner.run(context -> assertThat(context).doesNotHaveBean(HibernatePropertiesCustomizer.class));
  }

  @Test
  void 속성을_켜면_SQL_계측이_등록된다() {
    runner
        .withPropertyValues("request-log.count-sql-statements=true")
        .run(context -> assertThat(context).hasSingleBean(HibernatePropertiesCustomizer.class));
  }

  @Test
  void 요청_완료_로그_필터는_속성과_무관하게_항상_등록된다() {
    runner.run(context -> assertThat(context).hasSingleBean(RequestCompletionLoggingFilter.class));
  }
}
