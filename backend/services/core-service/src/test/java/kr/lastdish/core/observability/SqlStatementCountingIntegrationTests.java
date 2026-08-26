package kr.lastdish.core.observability;

import static org.assertj.core.api.Assertions.assertThat;

import kr.lastdish.common.mvc.SqlStatementCounter;
import kr.lastdish.common.mvc.SqlStatementCountingConfiguration;
import kr.lastdish.core.dish.infrastructure.DishJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * 계측이 실제 Hibernate 실행 경로에 붙는지 확인한다.
 *
 * <p>단위 테스트는 인스펙터를 직접 불러 검증하므로 "Hibernate가 이 인스펙터를 실제로 호출하는가"는 증명하지 못한다. 그 전제가 틀리면 계측값이 항상 0이 되어 측정
 * 전체가 무의미해지므로 실제 JPA 위에서 한 번 확인한다.
 */
@DataJpaTest
@Import({
  SqlStatementCountingConfiguration.class,
  SqlStatementCountingIntegrationTests.슬라이스용캐시설정.class
})
@TestPropertySource(properties = "request-log.count-sql-statements=true")
class SqlStatementCountingIntegrationTests {

  @Autowired private DishJpaRepository dishJpaRepository;

  /**
   * {@code CoreServiceApplication}의 {@code @EnableCaching} 때문에 캐시 프록시가 만들어지는데, JPA 슬라이스에는 캐시 자동 설정이
   * 빠져 있어 {@code CacheManager}가 없다. 이 테스트가 보려는 것은 캐시가 아니므로 가장 단순한 구현을 넣어 준다.
   */
  @TestConfiguration(proxyBeanMethods = false)
  static class 슬라이스용캐시설정 {

    @Bean
    CacheManager cacheManager() {
      return new ConcurrentMapCacheManager();
    }
  }

  @AfterEach
  void 카운터를_비운다() {
    SqlStatementCounter.clear();
  }

  @Test
  void 실제_조회가_요청당_SQL_수에_잡힌다() {
    SqlStatementCounter.start();

    dishJpaRepository.findByIdAndIsDeletedFalse(1L);

    assertThat(SqlStatementCounter.count()).hasValue(1);
  }

  @Test
  void 조회를_두_번_하면_두_번_잡힌다() {
    SqlStatementCounter.start();

    dishJpaRepository.findByIdAndIsDeletedFalse(1L);
    dishJpaRepository.findByIdAndIsDeletedFalse(2L);

    assertThat(SqlStatementCounter.count()).hasValue(2);
  }

  @Test
  void 계측을_시작하지_않은_조회는_세지_않는다() {
    dishJpaRepository.findByIdAndIsDeletedFalse(1L);

    assertThat(SqlStatementCounter.count()).isEmpty();
  }
}
