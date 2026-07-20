package kr.lastdish.core.dish.presentation.dto;

import jakarta.validation.constraints.NotNull;
import kr.lastdish.core.dish.domain.DishStatus;

public record DishStatusRequest(
        @NotNull DishStatus dishStatus
        ) {
}
