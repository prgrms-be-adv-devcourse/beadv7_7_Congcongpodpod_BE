package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import kr.lastdish.core.dish.domain.Category;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.infrastructure.DishJpaRepository;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.infrastructure.OrderJpaRepository;
import kr.lastdish.core.payment.domain.deposit.Deposit;
import kr.lastdish.core.payment.domain.deposit.DepositHistory;
import kr.lastdish.core.payment.infrastructure.DepositHistoryRepository;
import kr.lastdish.core.payment.infrastructure.DepositRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class OrderConcurrencyIntegrationTest {

  @Autowired private OrderFacade orderFacade;
  @Autowired private OrderJpaRepository orderJpaRepository;
  @Autowired private DishJpaRepository dishJpaRepository;
  @Autowired private DepositRepository depositRepository;
  @Autowired private DepositHistoryRepository depositHistoryRepository;
  @Autowired private TransactionTemplate transactionTemplate;

  @AfterEach
  void tearDown() {
    transactionTemplate.executeWithoutResult(
        status -> {
          depositHistoryRepository.deleteAll();
          orderJpaRepository.deleteAll();
          dishJpaRepository.deleteAll();
          depositRepository.deleteAll();
        });
  }

  @Test
  void concurrentCancellationRestoresStockAndRefundsOnlyOnce() throws Exception {
    Long memberId = 1L;
    BigDecimal unitPrice = BigDecimal.valueOf(1_000);
    Long quantity = 2L;

    Long orderId =
        transactionTemplate.execute(
            status -> {
              Dish dish =
                  dishJpaRepository.save(
                      Dish.create(
                          10L,
                          "테스트 메뉴",
                          LocalDateTime.now(),
                          "테스트",
                          Category.KOREAN,
                          null,
                          3L,
                          BigDecimal.valueOf(2_000),
                          unitPrice));

              depositRepository.save(new Deposit(memberId, BigDecimal.valueOf(8_000)));

              Order order =
                  Order.create(
                      memberId,
                      dish.getStoreId(),
                      dish.getId(),
                      "010-1234-5678",
                      dish.getDishName(),
                      quantity,
                      unitPrice,
                      LocalTime.of(18, 0),
                      LocalTime.of(19, 0));
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

  private boolean completedSuccessfully(Future<Boolean> result) {
    try {
      return result.get();
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }
}
