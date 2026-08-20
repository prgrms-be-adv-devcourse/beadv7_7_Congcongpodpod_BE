package kr.lastdish.core.store.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class StoreTest {

  @Test
  void recalculatesNextClosingAtWithChangedBusinessHoursAndHoliday() {
    Store store = store(LocalTime.of(9, 0), LocalTime.of(22, 0));
    store.addHoliday(DayOfWeek.TUESDAY);

    store.rescheduleNextClosingAt(LocalDateTime.of(2026, 8, 10, 23, 0));

    assertThat(store.getNextClosingAt()).isEqualTo(LocalDateTime.of(2026, 8, 12, 22, 0));
  }

  @Test
  void treatsClosingTimeBeforeOpeningTimeAsNextDay() {
    Store store = store(LocalTime.of(18, 0), LocalTime.of(2, 0));

    store.rescheduleNextClosingAt(LocalDateTime.of(2026, 8, 10, 19, 0));

    assertThat(store.getNextClosingAt()).isEqualTo(LocalDateTime.of(2026, 8, 11, 2, 0));
  }

  @Test
  void calculatesPickupDeadlineFromBusinessDate() {
    Store store = store(LocalTime.of(22, 0), LocalTime.of(2, 0));

    assertThat(
            store.calculatePickupDeadline(LocalDateTime.of(2026, 8, 20, 3, 0), LocalTime.of(2, 0)))
        .isEqualTo(LocalDateTime.of(2026, 8, 20, 2, 0));
    assertThat(
            store.calculatePickupDeadline(LocalDateTime.of(2026, 8, 20, 1, 0), LocalTime.of(2, 0)))
        .isEqualTo(LocalDateTime.of(2026, 8, 20, 2, 0));
  }

  private Store store(LocalTime openTime, LocalTime closeTime) {
    return new Store(
        1L,
        "테스트 매장",
        "123-45-67890",
        "서울시 테스트 주소",
        "02-0000-0000",
        openTime,
        closeTime,
        BigDecimal.valueOf(37.5),
        BigDecimal.valueOf(127.0),
        Category.KOREAN,
        LocalDateTime.of(2026, 8, 10, 12, 0));
  }
}
