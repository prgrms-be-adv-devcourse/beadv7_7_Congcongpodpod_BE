package kr.lastdish.ai.infrastructure.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.lastdish.ai.application.StoreIndexerService;
import kr.lastdish.common.event.EventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StoreEventListener {

  private final StoreIndexerService storeIndexerService;
  private final ObjectMapper objectMapper;

  @KafkaListener(
      topics = {"STORE_CREATED", "STORE_INFO_CHANGED", "STORE_STATUS_CHANGED", "STORE_IS_DELETED"},
      groupId = "${spring.kafka.consumer.group-id:ai-service-group}")
  public void listen(EventMessage message) {
    log.info(
        "Kafka 이벤트 수신. eventType={}, aggregateId={}", message.eventType(), message.aggregateId());

    try {
      JsonNode payloadNode = objectMapper.readTree(message.payload());
      Long storeId = payloadNode.has("storeId") ? payloadNode.get("storeId").asLong() : null;

      if ("STORE_IS_DELETED".equals(message.eventType())) {
        if (storeId != null) {
          storeIndexerService.deleteStoreIndex(storeId);
        }
        return;
      }

      if (storeId != null) {
        storeIndexerService.renewStoreIndex(storeId, message.eventType());
      } else {
        log.warn("이벤트 메시지에 storeId가 존재하지 않습니다. eventId={}", message.eventId());
      }

    } catch (Exception e) {
      log.error("이벤트 처리 중 오류 발생. eventId={}", message.eventId(), e);
      throw new RuntimeException("이벤트 처리 실패", e);
    }
  }
}
