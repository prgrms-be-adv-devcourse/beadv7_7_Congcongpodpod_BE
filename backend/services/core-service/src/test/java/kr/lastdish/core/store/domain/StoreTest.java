package kr.lastdish.core.store.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
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
  void 픽업_시작과_종료_시간이_같으면_거절한다() {
    Store store = store(LocalTime.of(9, 0), LocalTime.of(22, 0));

    assertThatThrownBy(() -> store.validatePickupTime(LocalTime.of(18, 0), LocalTime.of(18, 0)))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.DISH_PICKUP_TIME_OUTSIDE_STORE_HOURS);
  }

  private Store store(LocalTime openTime, LocalTime closeTime) {
    return new Store(
        1L,
        "테스트 매장",
        "123-45-67890",
        "서울시 테스트 주소",
        "명정빌딩",
        "02-0000-0000",
        openTime,
        closeTime,
        BigDecimal.valueOf(37.5),
        BigDecimal.valueOf(127.0),
        Category.KOREAN,
        LocalDateTime.of(2026, 8, 10, 12, 0));
  }
}
