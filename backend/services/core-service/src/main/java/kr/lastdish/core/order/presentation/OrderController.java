package kr.lastdish.core.order.presentation;

import jakarta.validation.Valid;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.order.application.OrderFacade;
import kr.lastdish.core.order.presentation.dto.OrderCancelRequest;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import kr.lastdish.core.order.presentation.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {
  private final OrderFacade orderFacade;

  @PostMapping
  public ApiResponse<OrderResponse> createOrder(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestBody @Valid OrderCreateRequest request) {
    return ApiResponse.ok(orderFacade.payAndCreateOrder(memberId, request));
  }

  @PatchMapping("/{orderId}/cancel")
  public ApiResponse<OrderResponse> cancelOrder(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @PathVariable Long orderId,
      @RequestBody @Valid OrderCancelRequest request) {
    return ApiResponse.ok(orderFacade.cancelOrder(memberId, orderId, request));
  }
}
