package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.cart.domain.Cart;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.infrastructure.CartItemJpaRepository;
import kr.lastdish.core.cart.infrastructure.CartJpaRepository;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.deposit.domain.Deposit;
import kr.lastdish.core.deposit.domain.DepositHistory;
import kr.lastdish.core.deposit.domain.DepositHistoryRepository;
import kr.lastdish.core.deposit.domain.DepositRepository;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishStatus;
import kr.lastdish.core.dish.infrastructure.DishJpaRepository;
import kr.lastdish.core.order.domain.MemberSnapshot;
import kr.lastdish.core.order.domain.MemberSnapshotRepository;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.infrastructure.OrderJpaRepository;
import kr.lastdish.core.point.application.PointService;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.Store;
import kr.lastdish.core.store.infrastructure.StoreJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class OrderConcurrencyIntegrationTest {

  @Autowired private OrderFacade orderFacade;
  @Autowired private OrderJpaRepository orderJpaRepository;
  @Autowired private DishJpaRepository dishJpaRepository;
  @Autowired private DepositRepository depositRepository;
  @Autowired private DepositHistoryRepository depositHistoryRepository;
  @Autowired private CartJpaRepository cartJpaRepository;
  @Autowired private CartItemJpaRepository cartItemJpaRepository;
  @Autowired private StoreJpaRepository storeJpaRepository;
  @Autowired private TransactionTemplate transactionTemplate;
  @Autowired private MemberSnapshotRepository memberSnapshotRepository;
  @MockitoBean private PointService pointService;

  @AfterEach
  void tearDown() {
    transactionTemplate.executeWithoutResult(
        status -> {
          depositHistoryRepository.deleteAll();
          orderJpaRepository.deleteAll();
          cartItemJpaRepository.deleteAll();
          cartJpaRepository.deleteAll();
          dishJpaRepository.deleteAll();
          storeJpaRepository.deleteAll();
          depositRepository.deleteAll();
          for (long memberId = 1L; memberId <= 30L; memberId++) {
            memberSnapshotRepository.deleteByMemberId(memberId);
          }
        });
  }

  @Test
  void ordersAtStockBoundaryAllowOnlyRemainingStock() throws Exception {
    long initialStock = 5L;
    int competitorCount = 30;
    BigDecimal unitPrice = BigDecimal.valueOf(1_000);

    RaceFixture fixture =
        transactionTemplate.execute(
            status -> {
              Store store =
                  storeJpaRepository.save(
                      new Store(
                          99L,
                          "품절 경계 테스트 매장",
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
                          "품절 경계 테스트 메뉴",
                          LocalDateTime.now(),
                          "테스트",
                          "기타",
                          null,
                          initialStock,
                          BigDecimal.valueOf(2_000),
                          unitPrice,
                          LocalTime.MIN,
                          LocalTime.of(23, 59, 59)));

              List<Long> memberIds = new ArrayList<>();
              List<Long> cartItemIds = new ArrayList<>();
              for (long memberId = 1L; memberId <= competitorCount; memberId++) {
                Cart cart = cartJpaRepository.save(Cart.create(memberId));
                CartItem cartItem =
                    cartItemJpaRepository.save(
                        CartItem.create(
                            cart.getId(),
                            dish.getId(),
                            dish.getStoreId(),
                            dish.getDishName(),
                            unitPrice,
                            unitPrice,
                            1L,
                            LocalTime.MIN,
                            LocalTime.of(23, 59, 59),
                            dish.getAggregateVersion()));
                depositRepository.save(new Deposit(memberId, BigDecimal.valueOf(10_000)));
                memberIds.add(memberId);
                cartItemIds.add(cartItem.getId());
              }
              return new RaceFixture(memberIds, cartItemIds, dish.getId());
            });

    for (Long memberId : fixture.memberIds()) {
      memberSnapshotRepository.save(
          MemberSnapshot.create(memberId, "테스트 회원 " + memberId, "010-1234-5678"));
    }

    CountDownLatch start = new CountDownLatch(1);
    List<Future<Throwable>> results = new ArrayList<>();
    try (ExecutorService executor = Executors.newFixedThreadPool(competitorCount)) {
      for (int index = 0; index < competitorCount; index++) {
        Long memberId = fixture.memberIds().get(index);
        Long cartItemId = fixture.cartItemIds().get(index);
        results.add(executor.submit(() -> orderAfterSignal(start, memberId, cartItemId)));
      }
      start.countDown();
    }

    List<Throwable> failures = new ArrayList<>();
    for (Future<Throwable> result : results) {
      Throwable failure = result.get();
      if (failure != null) {
        failures.add(failure);
      }
    }

    assertThat(results.size() - failures.size()).isEqualTo(initialStock);
    assertThat(failures)
        .allSatisfy(
            failure -> {
              assertThat(failure).isInstanceOf(BusinessException.class);
              assertThat(((BusinessException) failure).getErrorCode())
                  .isIn(ErrorCode.DISH_NOT_ON_SALE, ErrorCode.INSUFFICIENT_STOCK);
            });

    Dish dish = dishJpaRepository.findById(fixture.dishId()).orElseThrow();
    long depositUses =
        depositHistoryRepository.findAll().stream()
            .filter(history -> history.getType() == DepositHistory.DepositType.USE)
            .count();

    assertThat(dish.getStockQuantity()).isZero();
    assertThat(dish.getDishStatus()).isEqualTo(DishStatus.SOLD_OUT);
    assertThat(orderJpaRepository.findAll()).hasSize((int) initialStock);
    assertThat(depositUses).isEqualTo(initialStock);
  }

  @Test
  void sameCartItemConcurrentOrdersAllowOnlyOneOrder() throws Exception {
    Long memberId = 1L;
    BigDecimal unitPrice = BigDecimal.valueOf(1_000);
    Long quantity = 2L;

    Long cartItemId =
        transactionTemplate.execute(
            status -> {
              Store store =
                  storeJpaRepository.save(
                      new Store(
                          99L,
                          "동시 주문 테스트 매장",
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
                          "동시 주문 테스트 메뉴",
                          LocalDateTime.now(),
                          "테스트",
                          "기타",
                          null,
                          10L,
                          BigDecimal.valueOf(2_000),
                          unitPrice,
                          LocalTime.MIN,
                          LocalTime.of(23, 59, 59)));
              Cart cart = cartJpaRepository.save(Cart.create(memberId));
              CartItem cartItem =
                  cartItemJpaRepository.save(
                      CartItem.create(
                          cart.getId(),
                          dish.getId(),
                          dish.getStoreId(),
                          dish.getDishName(),
                          unitPrice,
                          unitPrice,
                          quantity,
                          LocalTime.MIN,
                          LocalTime.of(23, 59, 59),
                          dish.getAggregateVersion()));
              depositRepository.save(new Deposit(memberId, BigDecimal.valueOf(10_000)));
              return cartItem.getId();
            });

    memberSnapshotRepository.save(MemberSnapshot.create(memberId, "테스트 회원", "010-1234-5678"));

    CountDownLatch start = new CountDownLatch(1);
    List<Future<Throwable>> results;
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      results =
          List.of(
              executor.submit(() -> orderAfterSignal(start, memberId, cartItemId)),
              executor.submit(() -> orderAfterSignal(start, memberId, cartItemId)));
      start.countDown();
    }

    List<Throwable> failures = new ArrayList<>();
    for (Future<Throwable> result : results) {
      Throwable failure = result.get();
      if (failure != null) {
        failures.add(failure);
      }
    }

    // 첫 요청이 CartItem을 잠근 채 주문을 완료한다. 두 번째 요청은 잠금 해제 후 삭제된
    // CartItem을 조회하므로 주문·재고 차감·결제를 시작하지 않고 비즈니스 예외로 종료된다.
    assertThat(failures).hasSize(1);
    System.err.println("\n=== 비관적 잠금으로 차단된 동일 CartItem 동시 주문 ===");
    failures.getFirst().printStackTrace(System.err);
    assertThat(failures.getFirst()).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) failures.getFirst()).getErrorCode())
        .isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND);
    assertThat(orderJpaRepository.findAll()).hasSize(1);
    assertThat(cartItemJpaRepository.findById(cartItemId)).isEmpty();
  }

  @Test
  void concurrentCancellationRestoresStockAndRefundsOnlyOnce() throws Exception {
    Long memberId = 1L;
    BigDecimal unitPrice = BigDecimal.valueOf(1_000);
    Long quantity = 2L;

    Long orderId =
        transactionTemplate.execute(
            status -> {
              Store store =
                  storeJpaRepository.save(
                      new Store(
                          99L,
                          "동시 취소 테스트 매장",
                          "123-45-67890",
                          "서울시 강남구",
                          "명정빌딩",
                          "02-1234-5678",
                          LocalTime.of(18, 0),
                          LocalTime.of(19, 0),
                          BigDecimal.valueOf(37.5),
                          BigDecimal.valueOf(127.0),
                          Category.KOREAN,
                          LocalDateTime.now()));
              Dish dish =
                  dishJpaRepository.save(
                      Dish.create(
                          store.getId(),
                          "테스트 메뉴",
                          LocalDateTime.now(),
                          "테스트",
                          "기타",
                          null,
                          3L,
                          BigDecimal.valueOf(2_000),
                          unitPrice,
                          LocalTime.of(18, 0),
                          LocalTime.of(19, 0)));

              depositRepository.save(new Deposit(memberId, BigDecimal.valueOf(8_000)));

              Order order =
                  Order.create(
                      memberId,
                      dish.getStoreId(),
                      dish.getId(),
                      "테스트 회원",
                      "010-1234-5678",
                      dish.getDishName(),
                      quantity,
                      unitPrice,
                      unitPrice,
                      BigDecimal.ZERO,
                      LocalTime.of(18, 0),
                      LocalTime.of(19, 0),
                      LocalDateTime.of(2026, 8, 10, 19, 0));
              order.paymentSuccess();
              return orderJpaRepository.save(order).getId();
            });

    CountDownLatch start = new CountDownLatch(1);

    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      List<Future<Boolean>> results =
          List.of(
              executor.submit(() -> cancelAfterSignal(start, memberId, orderId)),
              executor.submit(() -> cancelAfterSignal(start, memberId, orderId)));

      start.countDown();

      assertThat(results.stream().filter(this::completedSuccessfully).count()).isEqualTo(1);
    }

    Order order = orderJpaRepository.findById(orderId).orElseThrow();
    Dish dish = dishJpaRepository.findById(order.getDishId()).orElseThrow();
    Deposit deposit = depositRepository.findByMemberId(memberId).orElseThrow();
    List<DepositHistory> refunds =
        depositHistoryRepository.findAll().stream()
            .filter(history -> history.getType() == DepositHistory.DepositType.REFUND)
            .toList();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(dish.getStockQuantity()).isEqualTo(5L);
    assertThat(deposit.getBalance()).isEqualByComparingTo("10000");
    assertThat(refunds).hasSize(1);
  }

  private boolean cancelAfterSignal(CountDownLatch start, Long memberId, Long orderId) {
    try {
      start.await();
      orderFacade.cancelOrder(memberId, orderId);
      return true;
    } catch (Exception ignored) {
      return false;
    }
  }

  private Throwable orderAfterSignal(CountDownLatch start, Long memberId, Long cartItemId) {
    try {
      start.await();
      orderFacade.payAndCreateOrder(memberId, cartItemId, 0L, BigDecimal.ZERO);
      return null;
    } catch (Throwable throwable) {
      return throwable;
    }
  }

  private boolean completedSuccessfully(Future<Boolean> result) {
    try {
      return result.get();
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }

  private record RaceFixture(List<Long> memberIds, List<Long> cartItemIds, Long dishId) {}
}
