package kr.lastdish.core.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import kr.lastdish.common.api.exception.BusinessException;
import org.junit.jupiter.api.Test;

class OrderTest {

  @Test
  void paymentCanOnlyTransitionFromPendingToCompleted() {
    Order order = createOrder();

    order.paymentSuccess();

    assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
    assertThatThrownBy(order::paymentSuccess).isInstanceOf(BusinessException.class);
  }

  @Test
  void cancellationChangesOnlyOrderStatusAndDuplicateCancellationIsRejected() {
    Order order = createOrder();
    order.paymentSuccess();

    order.cancel(1L);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
    assertThatThrownBy(() -> order.cancel(1L)).isInstanceOf(BusinessException.class);
  }

  @Test
  void cancellationDoesNotChangePendingPaymentStatus() {
    Order order = createOrder();

    order.cancel(1L);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
  }

  private Order createOrder() {
    return Order.create(
        1L,
        20L,
        30L,
        "테스트 회원",
        "010-1234-5678",
        "김밥",
        2L,
        BigDecimal.valueOf(5_000),
        LocalTime.of(18, 0),
        LocalTime.of(19, 0),
        LocalDateTime.of(2026, 8, 10, 19, 0));
  }
}
