package kr.lastdish.core.store.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import kr.lastdish.core.store.application.dto.InternalStoreResult;
import kr.lastdish.core.store.domain.Category;
import kr.lastdish.core.store.domain.StoreStatus;
import org.junit.jupiter.api.Test;

class InternalStoreResponseContractTest {

  @Test
  void renewal_response_contains_next_closing_at() {
    assertThat(
            Arrays.stream(InternalStoreResponse.class.getRecordComponents())
                .map(RecordComponent::getName))
        .contains("nextClosingAt");
  }

  @Test
  void renewal_response_maps_next_closing_at() {
    LocalDateTime nextClosingAt = LocalDateTime.of(2026, 8, 20, 22, 0);
    InternalStoreResult result =
        new InternalStoreResult(
            1L,
            2L,
            "테스트 매장",
            "서울시 테스트 주소",
            LocalTime.of(9, 0),
            LocalTime.of(22, 0),
            nextClosingAt,
            StoreStatus.OPEN,
            BigDecimal.valueOf(37.5),
            BigDecimal.valueOf(127.0),
            Category.KOREAN,
            List.of(),
            null);

    InternalStoreResponse response = InternalStoreResponse.from(result);

    assertThat(response.nextClosingAt()).isEqualTo(nextClosingAt);
  }
}
