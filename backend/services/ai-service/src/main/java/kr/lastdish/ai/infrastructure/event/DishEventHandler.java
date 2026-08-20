package kr.lastdish.ai.infrastructure.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.lastdish.ai.application.DishIndexerService;
import kr.lastdish.ai.domain.document.DishDocument;
import kr.lastdish.ai.infrastructure.event.dto.DishEventPayload;
import kr.lastdish.common.event.EventHandler;
import kr.lastdish.common.event.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DishEventHandler implements EventHandler {

  private final DishIndexerService dishIndexerService;
  private final ObjectMapper objectMapper;

  @Override
  public String consumerId() {
    return "ai-service-dish-indexer";
  }

  @Override
  public String eventType() {
    return "DISH_IS_CREATED";
  }

  @Override
  public void handle(EventMessage message) {
    try {
      // 1. JSON payload 역직렬화
      DishEventPayload payload = objectMapper.readValue(message.payload(), DishEventPayload.class);

      // 2. EventMessage metadata 및 payload를 기반으로 DishDocument 생성
      DishDocument document =
          DishDocument.builder()
              .dishId(payload.dishId())
              .storeId(payload.storeId())
              .storeName(payload.storeName())
              .dishName(payload.dishName())
              .description(payload.description())
              .thumbnailUrl(payload.thumbnailUrl())
              .stockQuantity(payload.stockQuantity())
              .dishStatus(payload.dishStatus())
              .dishPrice(payload.dishPrice())
              .discountPrice(payload.discountPrice())
              .registeredAt(payload.registeredAt())
              .version(message.aggregateVersion())
              .build();

      // 3. 단건 색인 실행, 내부에서 version 비교 후 갱신
      dishIndexerService.indexSingleDish(document);

    } catch (Exception e) {
      log.error("Dish 이벤트 처리 중 오류 발생. eventId={}", message.eventId(), e);
      throw new RuntimeException("Dish 이벤트 처리 실패", e);
    }
  }
}
