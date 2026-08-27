package kr.lastdish.common.mvc;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 요청당 SQL 실행 수 계측을 Hibernate에 붙인다. 설정을 켠 서비스에서만 등록된다.
 *
 * <p>계측은 SQL 한 문장마다 호출되므로 상시로 켜 둘 성격이 아니다. 그래서 기본은 꺼짐이고, 측정하는 동안만 {@code
 * request-log.count-sql-statements=true}로 켠다. 재배포 없이 설정만 바꿔 되돌릴 수 있도록 코드가 아니라 속성으로 가른다.
 *
 * <p>JPA를 쓰지 않는 서비스에도 이 모듈이 들어가므로 {@link StatementInspector}가 클래스패스에 있을 때만 등록한다. 조건 없이 등록하면 그런
 * 서비스에서 클래스를 찾지 못해 기동이 깨진다.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(StatementInspector.class)
@ConditionalOnProperty(name = "request-log.count-sql-statements", havingValue = "true")
public class SqlStatementCountingConfiguration {

  /** Hibernate 설정에 계측용 인스펙터를 끼워 넣는다. */
  @Bean
  public HibernatePropertiesCustomizer sqlStatementCountingCustomizer() {
    return hibernateProperties ->
        hibernateProperties.put(
            AvailableSettings.STATEMENT_INSPECTOR, new SqlStatementCountingInspector());
  }
}
