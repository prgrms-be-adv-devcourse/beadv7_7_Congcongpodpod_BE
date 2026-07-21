package kr.lastdish.core.order.presentation;

import kr.lastdish.core.common.response.ApiResponse;
import kr.lastdish.core.order.application.OrderFacade;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import kr.lastdish.core.order.presentation.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderFacade orderFacade;

    @PostMapping
    public ApiResponse<OrderResponse> createOrder(
            @RequestBody OrderCreateRequest request
    ) {
        return ApiResponse.ok(orderFacade.payAndCreateOrder(request));
    }
}
