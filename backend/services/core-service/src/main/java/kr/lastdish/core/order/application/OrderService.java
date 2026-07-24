package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orderRepository;
  private final PickupCodeGenerator pickupCodeGenerator;
  private static final int MAX_PICKUP_CODE_RETRY = 5;

  public Order createOrder(Long memberId, OrderCreateRequest request) {
    Order order =
        Order.create(
            memberId,
            request.storeId(),
            request.dishId(),
            // request.memberName(),
            request.phone(),
            request.dishName(),
            request.quantity(),
            request.unitPrice(),
            request.pickupStartAt(),
            request.pickupEndAt());

    return orderRepository.save(order);
  }

  public OrderResponse completePayment(Long orderId) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);
    order.paymentSuccess();
    return OrderResponse.from(order);
  }

  public Order cancelOrder(Long memberId, Long orderId) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);
    order.cancel(memberId);
    return order;
  }

  private static final List<OrderStatus> SETTLEMENT_TARGET_STATUSES =
      List.of(OrderStatus.PICKED_UP, OrderStatus.NO_SHOW);

  @Transactional(readOnly = true)
  public List<OrderSettlementInfo> findSettlementOrders(
      Long storeId, LocalDateTime periodStart, LocalDateTime periodEnd) {
    // validatePeriod(storeId, periodStart, periodEnd);

    return orderRepository
        .findSettlementTargetOrders(storeId, SETTLEMENT_TARGET_STATUSES, periodStart, periodEnd)
        .stream()
        .map(this::toSettlementInfo)
        .toList();
  }

  private OrderSettlementInfo toSettlementInfo(Order order) {
    return new OrderSettlementInfo(
        order.getId(), order.getStoreId(), order.getTotalPrice(), order.getUpdatedAt());
  }

  private String generatePickupCode(Long storeId) {
    for (int retry = 0; retry < MAX_PICKUP_CODE_RETRY; retry++) {
      String pickupCode = pickupCodeGenerator.generate();

      if (!orderRepository.validateActivePickUpCode(storeId, pickupCode)) {
        return pickupCode;
      }
    }

    throw new BusinessException(ErrorCode.PICKUP_CODE_GENERATION_FAILED);
  }

  @Transactional
  // 주문 접수 - 픽업 코드 발급
  public OrderReceptionResponse acceptOrder(Long orderId) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);
    String pickupCode = generatePickupCode(order.getStoreId());
    order.issuePickupCode(pickupCode);
    return OrderReceptionResponse.from(order);
  }

  @Transactional
  public PickupStatusResponse updatePickupStatus(Long orderId, PickupStatusRequest request) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);

    switch (request.status()) {
      case PICKED_UP -> order.completePickup();
      case NO_SHOW -> order.markNoShow();
      default -> throw new BusinessException(CommonErrorCode.INVALID_STATE);
    }

    return PickupStatusResponse.from(order);
  }

  @Transactional(readOnly = true)
  public OrderResponse getEachOrder(Long memberId, Long orderId) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);
    order.validateOwner(memberId);
    return OrderResponse.from(order);
  }

  public PickupCodeResponse getPickupCode(Long orderId, Long memberId) {
    Order order = orderRepository.findPickupAvailableOrder(orderId, memberId);
    return PickupCodeResponse.from(order);
  }

  @Transactional(readOnly = true)
  public Page<OrderResponse> getMyOrders(Long memberId, OrderStatus status, Pageable pageable) {
    return orderRepository
        .findAllByMemberIdAndStatus(memberId, status, pageable)
        .map(OrderResponse::from);
  }

  @Transactional(readOnly = true)
  public Page<OrderResponse> getStoreOrders(Long storeId, OrderStatus status, Pageable pageable) {
    return orderRepository
        .findAllByStoreIdAndStatus(storeId, status, pageable)
        .map(OrderResponse::from);
  }
}
