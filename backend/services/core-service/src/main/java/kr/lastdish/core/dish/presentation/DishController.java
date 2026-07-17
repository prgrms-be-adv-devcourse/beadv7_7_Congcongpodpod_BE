package kr.lastdish.core.dish.presentation;

import kr.lastdish.core.dish.application.DishService;
import kr.lastdish.core.common.response.ApiResponse;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishCreateResponse;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dishes")
public class DishController {
    private final DishService dishService;

    @PostMapping
    public ApiResponse<DishCreateResponse> createDish(
            @RequestBody DishCreateRequest request
    ) {
        return ApiResponse.ok(dishService.createDish(request));
    }
}
