package kr.lastdish.core.order.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import kr.lastdish.common.event.EventMessage;
import kr.lastdish.core.order.domain.OrderHistory;
import kr.lastdish.core.order.domain.OrderHistoryRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.domain.event.OrderStatusChangedPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

class OrderStatusChangedMessageHandlerTest {

  @Test
  @DisplayName("주문 상태 변경 이벤트를 소비하면 이벤트 내용으로 주문 이력을 저장한다")
  void handle_savesOrderHistoryFromEvent() throws Exception {
    // given: RESERVED 상태가 담긴 직렬화 이벤트 메시지를 준비한다.
    ObjectMapper objectMapper = mock(ObjectMapper.class);
    OrderHistoryRepository repository = mock(OrderHistoryRepository.class);
    OrderStatusChangedMessageHandler handler =
        new OrderStatusChangedMessageHandler(objectMapper, repository);
    Instant occurredAt = Instant.parse("2026-08-10T00:00:00Z");
    EventMessage message =
        new EventMessage(
            UUID.randomUUID(), "ORDER_STATUS_CHANGED", "ORDER", 10L, 1L, 1, "{}", occurredAt);
    when(objectMapper.readValue("{}", OrderStatusChangedPayload.class))
        .thenReturn(new OrderStatusChangedPayload(20L, OrderStatus.RESERVED));

    // when: 주문 상태 변경 메시지를 처리한다.
    handler.handle(message);

    // then: 이벤트 발생 시각과 주문 상태가 보존된 OrderHistory가 저장되어야 한다.
    ArgumentCaptor<OrderHistory> captor = ArgumentCaptor.forClass(OrderHistory.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getOrderId()).isEqualTo(10L);
    assertThat(captor.getValue().getMemberId()).isEqualTo(20L);
    assertThat(captor.getValue().getStatus()).isEqualTo(OrderStatus.RESERVED);
    assertThat(captor.getValue().getOrderUpdatedAt())
        .isEqualTo(LocalDateTime.ofInstant(occurredAt, ZoneId.systemDefault()));
  }
}
