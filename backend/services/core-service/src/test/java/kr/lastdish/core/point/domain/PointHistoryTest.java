package kr.lastdish.core.point.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PointHistoryTest {

    @Test
    void recordEarn_호출하면_EARN_이력이_생성된다() {
        PointHistory history =
                PointHistory.recordEarn(1L, 100L, new BigDecimal("500"), new BigDecimal("500"));

        assertThat(history.getMemberId()).isEqualTo(1L);
        assertThat(history.getOrderId()).isEqualTo(100L);
        assertThat(history.getType()).isEqualTo(PointType.EARN);
        assertThat(history.getAmount()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(history.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("500"));
        assertThat(history.getExpiresAt()).isAfter(LocalDateTime.now().plusMonths(2));
        assertThat(history.getBalanceAfter()).isEqualByComparingTo(new BigDecimal("500"));
    }

    @Test
    void recordUse_호출하면_USE_이력이_생성되고_remainingAmount와_expiresAt은_없다() {
        PointHistory history =
                PointHistory.recordUse(1L, 200L, new BigDecimal("300"), new BigDecimal("200"));

        assertThat(history.getType()).isEqualTo(PointType.USE);
        assertThat(history.getRemainingAmount()).isNull();
        assertThat(history.getExpiresAt()).isNull();
    }

    @Test
    void consume_호출하면_remainingAmount가_차감된다() {
        PointHistory history =
                PointHistory.recordEarn(1L, 100L, new BigDecimal("500"), new BigDecimal("500"));

        history.consume(new BigDecimal("200"));

        assertThat(history.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("300"));
    }
}