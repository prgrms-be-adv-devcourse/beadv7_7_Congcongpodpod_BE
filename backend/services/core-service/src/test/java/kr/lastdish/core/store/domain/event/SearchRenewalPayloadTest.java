package kr.lastdish.core.store.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import kr.lastdish.core.dish.domain.event.DishCreatedPayload;
import org.junit.jupiter.api.Test;

class SearchRenewalPayloadTest {

  @Test
  void store_created_event_uses_store_id_only_payload() {
    Long storeId = 10L;
    StoreCreatedEvent event =
        new StoreCreatedEvent(
            UUID.randomUUID(),
            StoreCreatedEvent.SCHEMA_VERSION,
            storeId,
            1L,
            new StoreCreatedPayload(storeId),
            Instant.now());

    assertThat(event.eventType()).isEqualTo("STORE_CREATED");
    assertThat(event.aggregateType()).isEqualTo("STORE");
    assertThat(event.aggregateId()).isEqualTo(storeId);
    assertThat(event.payload().storeId()).isEqualTo(storeId);
    assertContainsOnlyStoreId(StoreCreatedPayload.class);
  }

  @Test
  void store_changed_payload_contains_only_store_id() {
    assertContainsOnlyStoreId(StoreChangedPayload.class);
  }

  @Test
  void store_status_changed_payload_contains_only_store_id() {
    assertContainsOnlyStoreId(StoreStatusChangedPayload.class);
  }

  @Test
  void store_deleted_payload_contains_only_store_id() {
    assertContainsOnlyStoreId(StoreDeletedPayload.class);
  }

  @Test
  void dish_created_payload_contains_only_store_id() {
    assertContainsOnlyStoreId(DishCreatedPayload.class);
  }

  private void assertContainsOnlyStoreId(Class<?> payloadType) {
    assertThat(Arrays.stream(payloadType.getRecordComponents()).map(RecordComponent::getName))
        .containsExactly("storeId");
  }
}
