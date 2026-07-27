package kr.lastdish.core.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
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
  void cancellationIsIdempotentAndRefundsOnlyOnce() {
    Order order = createOrder();
    order.paymentSuccess();

    assertThat(order.cancel(1L)).isTrue();
    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
    assertThat(order.cancel(1L)).isFalse();
  }

  @Test
  void pendingPaymentOrderCannotBeCancelled() {
    Order order = createOrder();

    assertThatThrownBy(() -> order.cancel(1L)).isInstanceOf(BusinessException.class);
  }

  private Order createOrder() {
    return Order.create(
        1L,
        20L,
        30L,
        "010-1234-5678",
        "김밥",
        2L,
        BigDecimal.valueOf(5_000),
        LocalTime.of(18, 0),
        LocalTime.of(19, 0));
  }
}
