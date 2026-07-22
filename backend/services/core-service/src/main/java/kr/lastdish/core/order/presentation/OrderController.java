package kr.lastdish.core.order.presentation;

import jakarta.validation.Valid;
import kr.lastdish.core.common.response.ApiResponse;
import kr.lastdish.core.order.application.OrderFacade;
import kr.lastdish.core.order.presentation.dto.*;
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
      @RequestHeader("X-Authenticated-Member-Id") Long memberId, @PathVariable Long orderId) {
    return ApiResponse.ok(orderFacade.cancelOrder(memberId, orderId));
  }

  // 매장 주문 접수
  @PostMapping("/{orderId}/accept")
  public ApiResponse<OrderReceptionResponse> acceptOrder(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestHeader("X-Authenticated-Role") String role,
      @PathVariable Long orderId) {
    return ApiResponse.ok(orderFacade.acceptOrder(memberId, role, orderId));
  }

  // 매장 주문 반려
  @PostMapping("/{orderId}/reject")
  public ApiResponse<Void> rejectOrder(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestHeader("X-Authenticated-Role") String role,
      @PathVariable Long orderId,
      @RequestBody @Valid OrderRejectRequest request) {
    orderFacade.rejectOrder(memberId, role, orderId, request);
    return ApiResponse.ok();
  }
}
