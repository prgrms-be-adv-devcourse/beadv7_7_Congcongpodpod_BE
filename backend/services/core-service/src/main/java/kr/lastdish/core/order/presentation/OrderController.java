package kr.lastdish.core.order.presentation;

import jakarta.validation.Valid;
import kr.lastdish.common.api.response.ApiResponse;
import kr.lastdish.core.order.application.OrderFacade;
import kr.lastdish.core.order.application.OrderService;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {
  private final OrderFacade orderFacade;
  private final OrderService orderService;

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
  public ApiResponse<OrderRejectResponse> rejectOrder(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestHeader("X-Authenticated-Role") String role,
      @PathVariable Long orderId,
      @RequestBody @Valid OrderRejectRequest request) {
    return ApiResponse.ok(orderFacade.rejectOrder(memberId, role, orderId, request));
  }

  // 매장 픽업 처리
  @PatchMapping("/{orderId}/pickup")
  public ApiResponse<PickupStatusResponse> updatePickupStatus(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestHeader("X-Authenticated-Role") String role,
      @PathVariable Long orderId,
      @RequestBody @Valid PickupStatusRequest request) {
    return ApiResponse.ok(orderFacade.updateOrder(memberId, role, orderId, request));
  }

  @GetMapping("/{orderId}")
  public ApiResponse<OrderResponse> getEachOrder(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId, @PathVariable Long orderId) {
    return ApiResponse.ok(orderService.getEachOrder(memberId, orderId));
  }

  @GetMapping("/{orderId}/pickupCode")
  public ApiResponse<PickupCodeResponse> getPickupCode(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId, @PathVariable Long orderId) {
    return ApiResponse.ok(orderService.getPickupCode(memberId, orderId));
  }

  @GetMapping
  public ApiResponse<Page<OrderResponse>> getMyOrders(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestParam(required = false) OrderStatus status,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(orderService.getMyOrders(memberId, status, pageable));
  }

  @GetMapping("/stores/{storeId}")
  public ApiResponse<Page<OrderResponse>> getStoreOrders(
      @RequestHeader("X-Authenticated-Member-Id") Long memberId,
      @RequestHeader("X-Authenticated-Role") String role,
      @PathVariable Long storeId,
      @RequestParam(required = false) OrderStatus status,
      @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(orderFacade.getStoreOrders(memberId, role, storeId, status, pageable));
  }
}
