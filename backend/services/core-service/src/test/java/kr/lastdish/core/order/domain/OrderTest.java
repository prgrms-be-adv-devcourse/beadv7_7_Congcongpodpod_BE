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

  @Test
  void 픽업_종료_전에는_노쇼_처리할_수_없다() {
    Order order = createPickupReadyOrder();

    assertThatThrownBy(() -> order.markNoShow(LocalDateTime.of(2026, 8, 10, 18, 59)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(kr.lastdish.core.common.exception.ErrorCode.ORDER_PICKUP_TIME_NOT_ENDED);
    assertThat(order.getStatus()).isEqualTo(OrderStatus.PICKUP_READY);
  }

  @Test
  void 픽업_종료_시각부터_노쇼_처리할_수_있다() {
    Order order = createPickupReadyOrder();

    order.markNoShow(LocalDateTime.of(2026, 8, 10, 19, 0));

    assertThat(order.getStatus()).isEqualTo(OrderStatus.NO_SHOW);
  }

  @Test
  void 픽업_완료_시각을_Order에_저장한다() {
    Order order = createPickupReadyOrder();
    LocalDateTime pickedUpAt = LocalDateTime.of(2026, 8, 10, 18, 30);

    order.completePickup(pickedUpAt);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    assertThat(order.getPickedUpAt()).isEqualTo(pickedUpAt);
  }

  private Order createPickupReadyOrder() {
    Order order = createOrder();
    order.paymentSuccess();
    order.issuePickupCode("123456");
    return order;
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
