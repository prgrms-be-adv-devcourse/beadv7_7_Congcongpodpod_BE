package kr.lastdish.core.order.application.event;

import java.time.LocalDateTime;
import java.time.ZoneId;
import kr.lastdish.common.event.EventMessage;
import kr.lastdish.core.order.domain.OrderHistory;
import kr.lastdish.core.order.domain.OrderHistoryRepository;
import kr.lastdish.core.order.domain.event.OrderStatusChangedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class OrderStatusChangedMessageHandler {

  private final ObjectMapper objectMapper;
  private final OrderHistoryRepository orderHistoryRepository;

  public void handle(EventMessage message) {
    OrderStatusChangedPayload payload;
    try {
      payload = objectMapper.readValue(message.payload(), OrderStatusChangedPayload.class);
    } catch (JacksonException exception) {
      throw new IllegalStateException("주문 상태 변경 이벤트 역직렬화에 실패했습니다.", exception);
    }

    LocalDateTime orderUpdatedAt =
        LocalDateTime.ofInstant(message.occurredAt(), ZoneId.systemDefault());
    orderHistoryRepository.save(
        OrderHistory.create(
            message.aggregateId(), payload.memberId(), payload.status(), orderUpdatedAt));
  }
}
