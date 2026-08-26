package kr.lastdish.core.order.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import kr.lastdish.common.event.DomainEvent;
import kr.lastdish.common.outbox.application.OutboxEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRejectReason;
import kr.lastdish.core.order.domain.event.OrderNotificationEvent;
import kr.lastdish.core.order.domain.event.OrderNotificationPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OrderNotificationEventWriterTest {

  private OutboxEventWriter outboxEventWriter;
  private OrderNotificationEventWriter writer;
  private Order order;

  @BeforeEach
  void setUp() {
    outboxEventWriter = mock(OutboxEventWriter.class);
    writer = new OrderNotificationEventWriter(outboxEventWriter);
    order = mock(Order.class);
    when(order.getId()).thenReturn(10L);
    when(order.getMemberId()).thenReturn(1L);
  }

  @Test
  void appendCreated_createsNotificationForSeller() {
    when(order.getDishName()).thenReturn("마감 할인 도시락");

    writer.appendCreated(order, 20L);

    assertEvent(
        new OrderNotificationPayload(
            20L, "ORDER_CREATED", "새로운 주문이 들어왔어요", "마감 할인 도시락 주문이 접수 대기 중입니다.", null, null, null));
  }

  @Test
  void appendCancelled_createsNotificationForSeller() {
    when(order.getDishName()).thenReturn("마감 할인 도시락");

    writer.appendCancelled(order, 20L);

    assertEvent(
        new OrderNotificationPayload(
            20L,
            "ORDER_CANCELLED",
            "주문이 취소됐어요",
            "마감 할인 도시락 주문이 주문자에 의해 취소되었습니다.",
            null,
            null,
            null));
  }

  @Test
  void appendAccepted_createsNotificationForCustomer() {
    writer.appendAccepted(order);

    assertEvent(
        new OrderNotificationPayload(
            1L, "ORDER_ACCEPTED", "주문이 접수됐어요", "매장에서 주문을 확인하고 상품을 준비하고 있어요.", null, "ORDER", 10L));
  }

  @Test
  void appendRejected_createsNotificationForCustomer() {
    OrderRejectReason reason = OrderRejectReason.OUT_OF_STOCK;

    writer.appendRejected(order, reason);

    assertEvent(
        new OrderNotificationPayload(
            1L, "ORDER_REJECTED", "주문이 반려됐어요", reason.getMessage(), null, "ORDER", 10L));
  }

  @Test
  void appendPickupStarted_createsNotificationForCustomer() {
    when(order.getDishName()).thenReturn("마감 할인 도시락");

    writer.appendPickupStarted(order);

    assertEvent(
        new OrderNotificationPayload(
            1L,
            "PICKUP_STARTED",
            "픽업 시간이 시작됐어요",
            "마감 할인 도시락 상품을 지금부터 픽업할 수 있어요.",
            null,
            "ORDER",
            10L));
  }

  @Test
  void appendPickupDeadlineSoon_createsNotificationForCustomer() {
    when(order.getPickupEndAt()).thenReturn(LocalTime.of(19, 0));

    writer.appendPickupDeadlineSoon(order);

    assertEvent(
        new OrderNotificationPayload(
            1L,
            "PICKUP_DEADLINE_SOON",
            "픽업 마감까지 15분 남았어요",
            "19:00까지 매장에서 상품을 픽업해주세요.",
            null,
            "ORDER",
            10L));
  }

  @Test
  void appendPickedUp_createsNotificationForCustomer() {
    writer.appendPickedUp(order);

    assertEvent(
        new OrderNotificationPayload(
            1L, "PICKED_UP", "픽업이 완료됐어요", "상품 픽업이 완료되었습니다. 이용해주셔서 감사합니다.", null, "ORDER", 10L));
  }

  @Test
  void appendNoShow_createsNotificationForCustomer() {
    writer.appendNoShow(order);

    assertEvent(
        new OrderNotificationPayload(
            1L, "ORDER_NO_SHOW", "미수령 처리됐어요", "픽업 시간이 지나 주문이 미수령 처리되었습니다.", null, "ORDER", 10L));
  }

  private void assertEvent(OrderNotificationPayload expectedPayload) {
    ArgumentCaptor<DomainEvent<?>> captor = ArgumentCaptor.forClass(DomainEvent.class);
    verify(outboxEventWriter).append(captor.capture());

    OrderNotificationEvent event = (OrderNotificationEvent) captor.getValue();
    assertThat(event.eventType()).isEqualTo(OrderNotificationEvent.EVENT_TYPE);
    assertThat(event.aggregateType()).isEqualTo("ORDER");
    assertThat(event.aggregateId()).isEqualTo(10L);
    assertThat(event.aggregateVersion()).isZero();
    assertThat(event.schemaVersion()).isEqualTo(OrderNotificationEvent.SCHEMA_VERSION);
    assertThat(event.eventId()).isNotNull();
    assertThat(event.occurredAt()).isNotNull();
    assertThat(event.payload()).isEqualTo(expectedPayload);
    verify(order, never()).nextEventVersion();
  }
}
