package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import kr.lastdish.core.cart.domain.Cart;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.infrastructure.CartItemJpaRepository;
import kr.lastdish.core.cart.infrastructure.CartJpaRepository;
import kr.lastdish.core.deposit.domain.Deposit;
import kr.lastdish.core.deposit.domain.DepositHistoryRepository;
import kr.lastdish.core.deposit.domain.DepositRepository;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.infrastructure.DishJpaRepository;
import kr.lastdish.core.order.domain.MemberSnapshot;
import kr.lastdish.core.order.domain.MemberSnapshotRepository;
import kr.lastdish.core.order.infrastructure.OrderJpaRepository;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.infrastructure.StoreJpaRepository;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 주문 생성 한 건이 실행하는 SQL을 문장 단위로 기록해 무엇이 몇 번 나가는지 분해한다.
 *
 * <p>운영 실측(2026-08-28)에서 {@code POST /orders/cartItems/{id}}가 요청당 24쿼리였다. 항목 수를 바꿔도 늘지 않아 N+1은
 * 아니지만, 24개가 각각 무엇인지는 세지 않아 알 수 없었다. 요청당 실행 수만 남기는 운영 계측으로는 개수까지가 한계다.
 *
 * <p>그래서 {@link StatementInspector}가 SQL 문장 자체를 받는다는 점을 이용해 같은 자리에서 문장을 모은다. 운영 계측({@code
 * SqlStatementCountingInspector})은 세기만 하고 통과시키는데, 여기서는 세는 대신 적는다.
 *
 * <p>이 테스트의 목적은 판정이 아니라 <b>분해</b>다. 어떤 쿼리가 업무상 필요하고 어떤 것이 줄일 수 있는지는 목록을 본 뒤에 정한다.
 */
@SpringBootTest
@Import(OrderCreationQueryCountTests.기록용인스펙터설정.class)
class OrderCreationQueryCountTests {

  @Autowired private OrderFacade orderFacade;
  @Autowired private OrderJpaRepository orderJpaRepository;
  @Autowired private DishJpaRepository dishJpaRepository;
  @Autowired private DepositRepository depositRepository;
  @Autowired private DepositHistoryRepository depositHistoryRepository;
  @Autowired private CartJpaRepository cartJpaRepository;
  @Autowired private CartItemJpaRepository cartItemJpaRepository;
  @Autowired private StoreJpaRepository storeJpaRepository;
  @Autowired private MemberSnapshotRepository memberSnapshotRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  private static final long MEMBER_ID = 7_001L;

  /**
   * SQL을 실행 순서대로 적어 두는 인스펙터를 Hibernate에 끼운다.
   *
   * <p>운영 계측과 같은 확장점을 쓰지만 목적이 다르다 — 운영은 개수만 남기고(문장을 남기면 로그가 폭증한다) 여기서는 문장을 남긴다.
   */
  @TestConfiguration(proxyBeanMethods = false)
  static class 기록용인스펙터설정 {

    @Bean
    HibernatePropertiesCustomizer 기록용인스펙터() {
      return properties -> properties.put(AvailableSettings.STATEMENT_INSPECTOR, RECORDER);
    }
  }

  /** 테스트가 한 스레드에서 돌지만, Hibernate가 다른 스레드에서 부를 여지를 남겨 동기화한다. */
  private static final List<String> RECORDED = Collections.synchronizedList(new ArrayList<>());

  private static volatile boolean recording = false;

  private static final StatementInspector RECORDER =
      sql -> {
        if (recording) {
          RECORDED.add(sql);
        }
        return sql;
      };

  @AfterEach
  void 정리한다() {
    recording = false;
    RECORDED.clear();
    transactionTemplate.executeWithoutResult(
        status -> {
          depositHistoryRepository.deleteAll();
          orderJpaRepository.deleteAll();
          cartItemJpaRepository.deleteAll();
          cartJpaRepository.deleteAll();
          dishJpaRepository.deleteAll();
          storeJpaRepository.deleteAll();
          depositRepository.deleteAll();
          memberSnapshotRepository.deleteByMemberId(MEMBER_ID);
        });
  }

  /**
   * 개수를 박아 두는 이유는, 쿼리가 늘어도 응답값은 그대로라 기능 테스트로는 잡히지 않기 때문이다.
   *
   * <p>이 값이 틀렸다고 나오면 먼저 출력된 목록을 본다. 업무 로직이 늘어 정당하게 증가한 것이면 숫자를 올리고, 아래 두 검사에 걸린 것이면 되돌린다.
   */
  private static final int 주문_생성_쿼리_수 = 16;

  @Test
  void 주문_생성_쿼리_수가_늘지_않는다() {
    Long cartItemId = 준비한다();

    recording = true;
    orderFacade.payAndCreateOrder(MEMBER_ID, cartItemId, 0L, BigDecimal.ZERO);
    recording = false;

    List<String> executed = List.copyOf(RECORDED);
    출력한다(executed);

    assertThat(executed).hasSize(주문_생성_쿼리_수);
  }

  @Test
  void 이벤트를_기록할_때_INSERT_앞에_SELECT가_붙지_않는다() {
    Long cartItemId = 준비한다();

    recording = true;
    orderFacade.payAndCreateOrder(MEMBER_ID, cartItemId, 0L, BigDecimal.ZERO);
    recording = false;

    // OutboxEvent가 Persistable을 버리면 save()가 merge()로 돌아가고, 이벤트 1건마다
    // "있는지 확인하는" SELECT가 하나씩 붙는다. 개수만 세면 원인이 안 보여서 따로 본다.
    assertThat(RECORDED.stream().filter(sql -> 요약한다(sql).equals("select outbox_events")))
        .as("이벤트 기록은 INSERT만 나가야 한다")
        .isEmpty();
  }

  @Test
  void 같은_매장을_두_번_읽지_않는다() {
    Long cartItemId = 준비한다();

    recording = true;
    orderFacade.payAndCreateOrder(MEMBER_ID, cartItemId, 0L, BigDecimal.ZERO);
    recording = false;

    // 영업 여부를 확인하며 이미 읽은 매장이다. 주인을 찾을 때 PK 조회를 쓰면 1차 캐시에서 나온다.
    // 파생 쿼리나 JPQL로 바꾸면 캐시를 못 타서 SELECT가 하나 더 붙는다.
    assertThat(RECORDED.stream().filter(sql -> 요약한다(sql).equals("select stores")))
        .as("매장은 한 번만 읽어야 한다")
        .hasSize(1);
  }

  @Test
  void 매장_주인을_찾을_때_휴무일까지_읽지_않는다() {
    Long cartItemId = 준비한다();

    recording = true;
    orderFacade.payAndCreateOrder(MEMBER_ID, cartItemId, 0L, BigDecimal.ZERO);
    recording = false;

    // memberId 하나만 필요한데 매장 엔티티를 통째로 가져오면 StoreResult를 만드는 과정에서
    // holidays가 지연 로딩된다. 주문 생성과 무관한 데이터다.
    assertThat(RECORDED.stream().filter(sql -> 요약한다(sql).contains("store_holidays")))
        .as("알림 수신자를 정하는 데 휴무일은 필요 없다")
        .isEmpty();
  }

  /** 주문 한 건에 필요한 최소 데이터를 만든다. 이 준비 구간의 SQL은 기록하지 않는다. */
  private Long 준비한다() {
    memberSnapshotRepository.save(MemberSnapshot.create(MEMBER_ID, "분해 테스트 회원", "010-1234-5678"));

    return transactionTemplate.execute(
        status -> {
          Store store =
              storeJpaRepository.save(
                  new Store(
                      MEMBER_ID + 1,
                      "분해 테스트 매장",
                      "123-45-67890",
                      "서울시 강남구",
                      "명정빌딩",
                      "02-1234-5678",
                      LocalTime.MIN,
                      LocalTime.of(23, 59, 59),
                      BigDecimal.valueOf(37.5),
                      BigDecimal.valueOf(127.0),
                      Category.KOREAN,
                      LocalDateTime.now()));

          Dish dish =
              dishJpaRepository.save(
                  Dish.create(
                      store.getId(),
                      "분해 테스트 메뉴",
                      LocalDateTime.now(),
                      "테스트",
                      "기타",
                      null,
                      10L,
                      BigDecimal.valueOf(2_000),
                      BigDecimal.valueOf(1_000),
                      LocalTime.MIN,
                      LocalTime.of(23, 59, 59)));

          Cart cart = cartJpaRepository.save(Cart.create(MEMBER_ID));
          CartItem cartItem =
              cartItemJpaRepository.save(
                  CartItem.create(
                      cart.getId(),
                      dish.getId(),
                      dish.getStoreId(),
                      dish.getDishName(),
                      BigDecimal.valueOf(1_000),
                      BigDecimal.valueOf(1_000),
                      1L,
                      LocalTime.MIN,
                      LocalTime.of(23, 59, 59),
                      dish.getAggregateVersion()));

          depositRepository.save(new Deposit(MEMBER_ID, BigDecimal.valueOf(10_000)));
          return cartItem.getId();
        });
  }

  /** 실행 순서대로 번호를 붙여 찍는다. 어떤 테이블을 몇 번 건드렸는지도 함께 센다. */
  private void 출력한다(List<String> executed) {
    StringBuilder report = new StringBuilder();
    report
        .append("\n=============== 주문 생성 SQL 분해 — 총 ")
        .append(executed.size())
        .append("문 ===============\n");

    for (int index = 0; index < executed.size(); index++) {
      report
          .append(String.format("%2d. ", index + 1))
          .append(executed.get(index).replaceAll("\\s+", " "))
          .append('\n');
    }

    report.append("\n--- 종류별 ---\n");
    java.util.Map<String, Integer> byShape = new java.util.LinkedHashMap<>();
    for (String sql : executed) {
      byShape.merge(요약한다(sql), 1, Integer::sum);
    }
    byShape.forEach((shape, count) -> report.append(String.format("%3d회  %s%n", count, shape)));

    System.out.println(report);
  }

  /** "select ... from orders ..." 같은 문장을 "select orders"처럼 줄여 종류별로 묶는다. */
  private String 요약한다(String sql) {
    String normalized = sql.replaceAll("\\s+", " ").trim().toLowerCase();
    String verb = normalized.split(" ")[0];
    java.util.regex.Matcher matcher =
        java.util.regex.Pattern.compile("(?:from|into|update) ([a-z_]+)").matcher(normalized);
    return matcher.find() ? verb + " " + matcher.group(1) : verb;
  }
}
