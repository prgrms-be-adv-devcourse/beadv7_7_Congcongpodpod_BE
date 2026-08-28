package kr.lastdish.core.store.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.common.mvc.SqlStatementCounter;
import kr.lastdish.common.mvc.SqlStatementCountingConfiguration;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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
 * 매장을 여러 건 조회할 때 {@code holidays} 때문에 매장 수만큼 SELECT가 더 나가지 않는지 확인한다.
 *
 * <p>{@code StoreQueryService.toSnapshot}이 매장마다 {@code getHolidays()}를 읽는다. 이 컬렉션이 지연 로딩이면 매장 하나당
 * SELECT가 한 번씩 더 나가고, 호출부는 그것을 알 수 없다. 2026-08-28 운영 실측에서 주문 목록 조회가 매장 31개에 34쿼리를 쓰고 있었고 원인이 이것이었다.
 *
 * <p>쿼리 수를 세는 테스트가 필요한 이유는, 결과 값이 같아도 비용이 다르기 때문이다. 반환 데이터만 검증하면 N+1이 다시 들어와도 통과한다.
 */
@DataJpaTest
@Import({
  SqlStatementCountingConfiguration.class,
  StoreJpaRepositoryHolidayFetchTests.슬라이스용캐시설정.class
})
@TestPropertySource(properties = "request-log.count-sql-statements=true")
class StoreJpaRepositoryHolidayFetchTests {

  @Autowired private StoreJpaRepository storeJpaRepository;
  @Autowired private EntityManager entityManager;

  /** JPA 슬라이스에는 캐시 자동 설정이 없어 {@code @EnableCaching}이 요구하는 빈을 최소 구현으로 넣어 준다. */
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

  private long 다음_회원번호 = 1L;

  private Store 매장을_만든다(String name, DayOfWeek... holidays) {
    long memberId = 다음_회원번호++;
    Store store =
        new Store(
            memberId,
            name,
            String.format("000-00-%05d", memberId),
            "서울특별시 서초구 효령로 289",
            "1층",
            "02-000-0000",
            LocalTime.of(9, 0),
            LocalTime.of(22, 0),
            new BigDecimal("37.485140"),
            new BigDecimal("127.015831"),
            Category.KOREAN,
            LocalDateTime.now());
    for (DayOfWeek holiday : holidays) {
      store.addHoliday(holiday);
    }
    return storeJpaRepository.save(store);
  }

  /** 영속성 컨텍스트에 남은 엔티티를 읽어 SQL이 안 나가는 일이 없도록 비운다. */
  private void 캐시를_비운다() {
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  @DisplayName("매장을 여러 건 조회해도 휴무일 때문에 쿼리가 늘지 않는다")
  void 휴무일_조회가_매장_수만큼_늘지_않는다() {
    List<Long> 매장_셋 =
        List.of(
            매장을_만든다("가게1", DayOfWeek.MONDAY).getId(),
            매장을_만든다("가게2", DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY).getId(),
            매장을_만든다("가게3").getId());
    캐시를_비운다();

    SqlStatementCounter.start();
    List<Store> stores = storeJpaRepository.findAllByIdInAndDeletedFalse(매장_셋);
    // 호출부가 하는 일을 그대로 따라 한다 — 여기서 지연 로딩이면 매장마다 SELECT가 더 나간다.
    stores.forEach(store -> store.getHolidays().size());

    assertThat(SqlStatementCounter.count())
        .as("매장 3건을 조회하고 휴무일까지 읽는 데 SELECT는 한 번이어야 한다")
        .hasValue(1);
  }

  @Test
  @DisplayName("매장이 늘어도 쿼리 수는 그대로다")
  void 매장이_늘어도_쿼리_수는_그대로다() {
    List<Long> 하나 = List.of(매장을_만든다("하나", DayOfWeek.SUNDAY).getId());
    List<Long> 다섯 =
        List.of(
            매장을_만든다("다섯1", DayOfWeek.MONDAY).getId(),
            매장을_만든다("다섯2", DayOfWeek.TUESDAY).getId(),
            매장을_만든다("다섯3", DayOfWeek.WEDNESDAY).getId(),
            매장을_만든다("다섯4", DayOfWeek.THURSDAY).getId(),
            매장을_만든다("다섯5", DayOfWeek.FRIDAY).getId());
    캐시를_비운다();

    SqlStatementCounter.start();
    storeJpaRepository.findAllByIdInAndDeletedFalse(하나).forEach(s -> s.getHolidays().size());
    int 하나일때 = SqlStatementCounter.count().orElseThrow();
    캐시를_비운다();

    SqlStatementCounter.start();
    storeJpaRepository.findAllByIdInAndDeletedFalse(다섯).forEach(s -> s.getHolidays().size());
    int 다섯일때 = SqlStatementCounter.count().orElseThrow();

    assertThat(다섯일때).as("매장이 1건에서 5건으로 늘어도 쿼리 수는 같아야 한다 (N+1이면 5건일 때 늘어난다)").isEqualTo(하나일때);
  }

  @Test
  @DisplayName("휴무일 값 자체는 그대로 읽힌다")
  void 휴무일_값이_보존된다() {
    Long 화수휴무 = 매장을_만든다("화수휴무", DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY).getId();
    캐시를_비운다();

    Store 조회된_매장 = storeJpaRepository.findAllByIdInAndDeletedFalse(List.of(화수휴무)).getFirst();

    assertThat(조회된_매장.getHolidays())
        .extracting(holiday -> holiday.getDayOfWeek())
        .containsExactlyInAnyOrder(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY);
  }
}
