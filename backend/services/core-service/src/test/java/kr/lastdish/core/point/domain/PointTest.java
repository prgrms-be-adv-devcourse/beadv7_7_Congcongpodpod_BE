package kr.lastdish.core.point.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PointTest {

    @Test
    void createDefault_호출하면_잔액0원인_포인트가_생성된다() {
        Point point = Point.createDefault(1L);

        assertThat(point.getMemberId()).isEqualTo(1L);
        assertThat(point.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void earn_호출하면_잔액이_증가한다() {
        Point point = Point.createDefault(1L);

        point.earn(new BigDecimal("1000"));

        assertThat(point.getBalance()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    void earn_금액이_0이하면_예외가_발생한다() {
        Point point = Point.createDefault(1L);

        assertThatThrownBy(() -> point.earn(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void use_잔액이_충분하면_차감된다() {
        Point point = Point.createDefault(1L);
        point.earn(new BigDecimal("1000"));

        point.use(new BigDecimal("300"));

        assertThat(point.getBalance()).isEqualByComparingTo(new BigDecimal("700"));
    }

    @Test
    void use_잔액이_부족하면_예외가_발생한다() {
        Point point = Point.createDefault(1L);
        point.earn(new BigDecimal("100"));

        assertThatThrownBy(() -> point.use(new BigDecimal("200")))
                .isInstanceOf(InsufficientPointException.class);
    }
}