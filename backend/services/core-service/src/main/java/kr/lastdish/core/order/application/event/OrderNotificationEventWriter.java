package kr.lastdish.core.order.application.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRejectReason;
import kr.lastdish.core.order.domain.event.OrderNotificationEvent;
import kr.lastdish.core.order.domain.event.OrderNotificationPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderNotificationEventWriter {

  private static final long NOT_APPLICABLE_VERSION = 0L;

  private final OutboxEventWriter outboxEventWriter;

  public void appendCancelled(Order order, Long sellerMemberId) {
    append(
        order,
        new OrderNotificationPayload(
            sellerMemberId,
            "ORDER_CANCELLED",
            "주문이 취소됐어요",
            order.getDishName() + " 주문이 주문자에 의해 취소되었습니다.",
            null,
            null,
            null));
  }

  public void appendAccepted(Order order) {
    append(
        order,
        new OrderNotificationPayload(
            order.getMemberId(),
            "ORDER_ACCEPTED",
            "주문이 접수됐어요",
            "매장에서 주문을 확인하고 상품을 준비하고 있어요.",
            null,
            "ORDER",
            order.getId()));
  }

  public void appendRejected(Order order, OrderRejectReason reason) {
    append(
        order,
        new OrderNotificationPayload(
            order.getMemberId(),
            "ORDER_REJECTED",
            "주문이 반려됐어요",
            reason.getMessage(),
            null,
            "ORDER",
            order.getId()));
  }

  public void appendPickupStarted(Order order) {
    append(
        order,
        new OrderNotificationPayload(
            order.getMemberId(),
            "PICKUP_STARTED",
            "픽업 시간이 시작됐어요",
            order.getDishName() + " 상품을 지금부터 픽업할 수 있어요.",
            null,
            "ORDER",
            order.getId()));
  }

  public void appendPickupDeadlineSoon(Order order) {
    append(
        order,
        new OrderNotificationPayload(
            order.getMemberId(),
            "PICKUP_DEADLINE_SOON",
            "픽업 마감까지 15분 남았어요",
            order.getPickupEndAt() + "까지 매장에서 상품을 픽업해주세요.",
            null,
            "ORDER",
            order.getId()));
  }

  public void appendPickedUp(Order order) {
    append(
        order,
        new OrderNotificationPayload(
            order.getMemberId(),
            "PICKED_UP",
            "픽업이 완료됐어요",
            "상품 픽업이 완료되었습니다. 이용해주셔서 감사합니다.",
            null,
            "ORDER",
            order.getId()));
  }

  public void appendNoShow(Order order) {
    append(
        order,
        new OrderNotificationPayload(
            order.getMemberId(),
            "ORDER_NO_SHOW",
            "미수령 처리됐어요",
            "픽업 시간이 지나 주문이 미수령 처리되었습니다.",
            null,
            "ORDER",
            order.getId()));
  }

  private void append(Order order, OrderNotificationPayload payload) {
    OrderNotificationEvent event =
        new OrderNotificationEvent(
            UUID.randomUUID(),
            OrderNotificationEvent.SCHEMA_VERSION,
            order.getId(),
            NOT_APPLICABLE_VERSION,
            payload,
            Instant.now());

    outboxEventWriter.append(event);
  }
}
