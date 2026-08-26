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

  private void append(Order order, OrderNotificationPayload payload) {
    OrderNotificationEvent event =
        new OrderNotificationEvent(
            UUID.randomUUID(),
            OrderNotificationEvent.SCHEMA_VERSION,
            order.getId(),
            order.nextEventVersion(),
            payload,
            Instant.now());

    outboxEventWriter.append(event);
  }
}
