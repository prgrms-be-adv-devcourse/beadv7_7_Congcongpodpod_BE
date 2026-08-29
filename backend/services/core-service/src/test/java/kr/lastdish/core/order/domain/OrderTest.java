package kr.lastdish.core.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class OrderTest {

  @Test
  void 사용_포인트를_제외한_금액을_사용_예치금으로_계산한다() {
    Order order = createOrder(BigDecimal.valueOf(3_000));

    assertThat(order.getTotalPrice()).isEqualByComparingTo("10000");
    assertThat(order.getUsedPoint()).isEqualByComparingTo("3000");
    assertThat(order.getUsedDeposit()).isEqualByComparingTo("7000");
  }

  @Test
  void 주문의_픽업_시간으로_마감_일시를_계산한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 20, 12, 0);

    assertThat(Order.calculatePickupDeadline(now, LocalTime.of(18, 0), LocalTime.of(19, 0)))
        .isEqualTo(LocalDateTime.of(2026, 8, 20, 19, 0));
  }

  @Test
  void 오늘_픽업_종료_후에는_다음날_마감_일시를_계산한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 20, 21, 0);

    assertThat(Order.calculatePickupDeadline(now, LocalTime.of(10, 0), LocalTime.of(20, 0)))
        .isEqualTo(LocalDateTime.of(2026, 8, 21, 20, 0));
  }

  @Test
  void 자정을_넘는_픽업은_종료_시간을_다음날로_계산한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 20, 23, 30);

    assertThat(Order.calculatePickupDeadline(now, LocalTime.of(23, 0), LocalTime.of(1, 0)))
        .isEqualTo(LocalDateTime.of(2026, 8, 21, 1, 0));
  }

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
    LocalDateTime noShowAt = LocalDateTime.of(2026, 8, 10, 19, 0);

    order.markNoShow(noShowAt);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.NO_SHOW);
    assertThat(order.getPickupResultAt()).isEqualTo(noShowAt);
  }

  @Test
  void 픽업_완료_시각을_Order에_저장한다() {
    Order order = createPickupReadyOrder();
    LocalDateTime pickedUpAt = LocalDateTime.of(2026, 8, 10, 18, 30);

    order.completePickup(pickedUpAt);

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PICKED_UP);
    assertThat(order.getPickupResultAt()).isEqualTo(pickedUpAt);
  }

  @Test
  void 픽업_대기_상태에서는_픽업_코드를_볼_수_있다() {
    Order order = createPickupReadyOrder();

    order.validatePickupCodeReadable();
  }

  @Test
  void 수락_전에는_픽업_코드를_볼_수_없고_현재_상태를_알려준다() {
    Order order = createOrder();

    assertThatThrownBy(order::validatePickupCodeReadable)
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("예약 완료");
  }

  @Test
  void 픽업이_끝나면_픽업_코드를_볼_수_없고_현재_상태를_알려준다() {
    Order order = createPickupReadyOrder();
    order.completePickup(LocalDateTime.of(2026, 8, 10, 18, 30));

    assertThatThrownBy(order::validatePickupCodeReadable)
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("픽업 완료");
  }

  /*
   * "주문이 없다"와 "지금은 볼 수 없다"를 구분하는 것이 이 검증의 목적이다.
   * 둘을 같은 오류로 뭉개면 호출자가 안내를 나눌 수 없다(이슈 #479).
   */
  @Test
  void 픽업_코드를_볼_수_없는_이유는_주문_부재와_다른_오류다() {
    Order order = createOrder();

    assertThatThrownBy(order::validatePickupCodeReadable)
        .isInstanceOf(BusinessException.class)
        .extracting(failure -> ((BusinessException) failure).getErrorCode())
        .isEqualTo(ErrorCode.ORDER_PICKUP_CODE_NOT_AVAILABLE);
  }

  private Order createPickupReadyOrder() {
    Order order = createOrder();
    order.paymentSuccess();
    order.issuePickupCode("123456");
    return order;
  }

  private Order createOrder() {
    return createOrder(BigDecimal.ZERO);
  }

  private Order createOrder(BigDecimal usedPoint) {
    return Order.create(
        1L,
        20L,
        30L,
        "테스트 회원",
        "010-1234-5678",
        "김밥",
        2L,
        BigDecimal.valueOf(5_000),
        BigDecimal.valueOf(5_000),
        usedPoint,
        LocalTime.of(18, 0),
        LocalTime.of(19, 0),
        LocalDateTime.of(2026, 8, 10, 19, 0));
  }
}
