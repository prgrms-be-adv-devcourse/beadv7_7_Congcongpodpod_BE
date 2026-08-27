package kr.lastdish.core.point.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.EventMessage;
import kr.lastdish.common.inbox.domain.InboxProcessingPolicy;
import kr.lastdish.core.order.domain.event.OrderPickedUpEvent;
import kr.lastdish.core.point.application.PointFacade;
import kr.lastdish.core.point.application.event.kafka.PointOrderPickedUpKafkaListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PointOrderPickedUpMessageHandlerTest {

  @Mock private PointFacade pointFacade;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private PointOrderPickedUpMessageHandler handler;

  @BeforeEach
  void setUp() {
    handler = new PointOrderPickedUpMessageHandler(objectMapper, pointFacade);
  }

  @Test
  void consumerId_eventType_processingPolicy가_올바르게_반환된다() {
    assertThat(handler.consumerId()).isEqualTo(PointOrderPickedUpKafkaListener.CONSUMER_ID);
    assertThat(handler.eventType()).isEqualTo(OrderPickedUpEvent.EVENT_TYPE);
    assertThat(handler.processingPolicy()).isEqualTo(InboxProcessingPolicy.IDEMPOTENT);
  }

  @Test
  void handle_정상_페이로드면_PointFacade를_올바른_인자로_호출한다() {
    Long orderId = 100L;
    String json =
        "{\"orderId\":100,\"memberId\":1,\"storeId\":2,"
            + "\"finalOrderAmount\":10000,\"savedAmount\":1000,\"pickupResultAt\":\"2026-08-24T12:00:00\"}";

    EventMessage message =
        new EventMessage(
            UUID.randomUUID(),
            OrderPickedUpEvent.EVENT_TYPE,
            "ORDER",
            orderId,
            1L,
            OrderPickedUpEvent.SCHEMA_VERSION,
            json,
            Instant.now());

    handler.handle(message);

    verify(pointFacade, times(1))
        .earnAndEvaluateLevel(1L, orderId, new BigDecimal("10000"), new BigDecimal("1000"));
  }

  @Test
  void handle_역직렬화에_실패하면_IllegalStateException을_던지고_PointFacade는_호출하지_않는다() {
    EventMessage message =
        new EventMessage(
            UUID.randomUUID(),
            OrderPickedUpEvent.EVENT_TYPE,
            "ORDER",
            100L,
            1L,
            OrderPickedUpEvent.SCHEMA_VERSION,
            "{ invalid json",
            Instant.now());

    Assertions.assertThrows(IllegalStateException.class, () -> handler.handle(message));

    verify(pointFacade, never()).earnAndEvaluateLevel(any(), any(), any(), any());
  }
}
