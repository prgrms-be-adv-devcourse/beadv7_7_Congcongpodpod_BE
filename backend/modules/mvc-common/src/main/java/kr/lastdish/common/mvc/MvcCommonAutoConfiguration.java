package kr.lastdish.common.mvc;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({
  GlobalExceptionHandler.class,
  RequestIdFilter.class,
  RequestCompletionLoggingFilter.class,
  // 등록 조건(JPA 존재 여부와 계측 속성)은 해당 설정 클래스가 직접 들고 있다.
  SqlStatementCountingConfiguration.class
})
public class MvcCommonAutoConfiguration {

  /**
   * 데이터 접근 예외 처리기는 spring-tx가 있을 때만 등록한다.
   *
   * <p>이 모듈은 JPA를 쓰지 않는 서비스도 함께 쓸 수 있다. 조건 없이 등록하면 그런 서비스에서 클래스를 찾지 못해 예외 처리 전체가 무너진다.
   */
  @Configuration(proxyBeanMethods = false)
  @ConditionalOnClass(DataIntegrityViolationException.class)
  @Import(DataIntegrityExceptionHandler.class)
  static class DataAccessExceptionHandlingConfiguration {}
}
