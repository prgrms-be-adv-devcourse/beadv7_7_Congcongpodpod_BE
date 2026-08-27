package kr.lastdish.core.dish.presentation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DishCreateRequestValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void imageKey가_null이어도_상품_생성_요청은_유효하다() {
    DishCreateRequest request =
        new DishCreateRequest(
            1L,
            "김치찌개",
            LocalDateTime.now(),
            "상품 설명",
            "한식",
            null,
            10L,
            BigDecimal.valueOf(10_000),
            BigDecimal.valueOf(7_000),
            LocalTime.of(18, 0),
            LocalTime.of(19, 0));

    assertThat(validator.validate(request)).isEmpty();
  }
}
