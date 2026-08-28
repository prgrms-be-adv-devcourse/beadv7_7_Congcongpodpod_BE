package kr.lastdish.common.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SqlStatementCountingConfigurationTests {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(SqlStatementCountingConfiguration.class));

  @Test
  void 기본값에서는_계측을_등록하지_않는다() {
    runner.run(context -> assertThat(context).doesNotHaveBean(HibernatePropertiesCustomizer.class));
  }

  @Test
  void 속성을_끄면_계측을_등록하지_않는다() {
    runner
        .withPropertyValues("request-log.count-sql-statements=false")
        .run(context -> assertThat(context).doesNotHaveBean(HibernatePropertiesCustomizer.class));
  }

  @Test
  void 속성을_켜면_계측을_등록한다() {
    runner
        .withPropertyValues("request-log.count-sql-statements=true")
        .run(context -> assertThat(context).hasSingleBean(HibernatePropertiesCustomizer.class));
  }

  @Test
  void 등록된_계측은_Hibernate_설정에_인스펙터를_넣는다() {
    runner
        .withPropertyValues("request-log.count-sql-statements=true")
        .run(
            context -> {
              Map<String, Object> 하이버네이트설정 = new HashMap<>();

              context.getBean(HibernatePropertiesCustomizer.class).customize(하이버네이트설정);

              assertThat(하이버네이트설정.get(AvailableSettings.STATEMENT_INSPECTOR))
                  .isInstanceOf(SqlStatementCountingInspector.class);
            });
  }
}
