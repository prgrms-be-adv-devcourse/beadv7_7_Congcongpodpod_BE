package kr.lastdish.ai.infrastructure.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import kr.lastdish.ai.application.StoreIndexerService;
import kr.lastdish.common.event.EventMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StoreEventListenerTest {

  @Mock private StoreIndexerService storeIndexerService;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private StoreEventListener storeEventListener;

  private EventMessage createEventMessage(
      String eventType, Long aggregateId, Map<String, Object> payloadMap)
      throws JsonProcessingException {
    String payloadJson = objectMapper.writeValueAsString(payloadMap);

    return new EventMessage(
        UUID.randomUUID(), eventType, "DISH", aggregateId, 1L, 1, payloadJson, Instant.now());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "DISH_IS_CREATED",
        "DISH_IS_UPDATED",
        "DISH_IS_DELETED",
        "STORE_CREATED",
        "STORE_INFO_CHANGED",
        "STORE_STATUS_CHANGED"
      })
  @DisplayName("갱신 대상 이벤트 수신 시 renewStoreIndex가 정상 호출된다.")
  void listen_renewalEvents_success(String eventType) throws Exception {
    // given
    Long storeId = 1L;
    Map<String, Object> payloadMap = Map.of("dishId", 100L, "storeId", storeId);
    EventMessage message = createEventMessage(eventType, 100L, payloadMap);

    // when
    storeEventListener.listen(message);

    // then
    verify(storeIndexerService).renewStoreIndex(eq(storeId), eq(eventType));
  }

  @Test
  @DisplayName("STORE_IS_DELETED 이벤트 수신 시 deleteStoreIndex가 정상 호출된다.")
  void listen_storeDeletedEvent_success() throws Exception {
    // given
    Long storeId = 1L;
    Map<String, Object> payloadMap = Map.of("storeId", storeId);
    EventMessage message = createEventMessage("STORE_IS_DELETED", storeId, payloadMap);

    // when
    storeEventListener.listen(message);

    // then
    verify(storeIndexerService).deleteStoreIndex(eq(storeId));
    verify(storeIndexerService, never()).renewStoreIndex(any(), any());
  }

  @Test
  @DisplayName("payload에 storeId가 없으면 아무 서비스 메서드도 호출되지 않는다.")
  void listen_missingStoreId_doesNothing() throws Exception {
    // given
    Map<String, Object> payloadMap = Map.of("dishId", 100L); // storeId 누락
    EventMessage message = createEventMessage("DISH_IS_CREATED", 100L, payloadMap);

    // when
    storeEventListener.listen(message);

    // then
    verify(storeIndexerService, never()).renewStoreIndex(any(), any());
    verify(storeIndexerService, never()).deleteStoreIndex(any());
  }

  @Test
  @DisplayName("이벤트 처리 중 예외가 발생하면 RuntimeException으로 재던져져 Kafka 재시도를 유발한다.")
  void listen_processingFails_rethrowsAsRuntimeException() throws Exception {
    // given
    Long storeId = 1L;
    Map<String, Object> payloadMap = Map.of("storeId", storeId);
    EventMessage message = createEventMessage("DISH_IS_UPDATED", 100L, payloadMap);

    willThrow(new RuntimeException("Core API 통신 장애"))
        .given(storeIndexerService)
        .renewStoreIndex(eq(storeId), eq("DISH_IS_UPDATED"));

    // when & then
    assertThatThrownBy(() -> storeEventListener.listen(message))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("이벤트 처리 실패");
  }
}
