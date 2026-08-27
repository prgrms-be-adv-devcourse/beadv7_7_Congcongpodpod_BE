package kr.lastdish.core.order.presentation.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderCreateRequestTest {

  @Test
  @DisplayName("사용 포인트를 입력하지 않으면 0으로 설정한다")
  void defaultsUsedPointToZero() {
    OrderCreateRequest request = new OrderCreateRequest(1L, null);

    assertThat(request.usedPoint()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
