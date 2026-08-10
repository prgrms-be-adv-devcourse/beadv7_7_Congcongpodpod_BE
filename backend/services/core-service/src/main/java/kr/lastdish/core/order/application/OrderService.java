package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.cart.application.dto.CartOrderSnapshot;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.order.application.dto.*;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orderRepository;
  private final OrderStatusChangedEventWriter orderStatusChangedEventWriter;
  private final PickupCodeGenerator pickupCodeGenerator;
  private static final int MAX_PICKUP_CODE_RETRY = 5;

  public Order createOrder(Long memberId, OrderMemberInfo memberInfo, CartOrderSnapshot cartItem) {
    Order order =
        Order.create(
            memberId,
            cartItem.storeId(),
            cartItem.dishId(),
            memberInfo.name(),
            memberInfo.phone(),
            cartItem.dishName(),
            cartItem.quantity(),
            cartItem.unitPrice(),
            cartItem.pickupStartAt(),
            cartItem.pickupEndAt());

    Order savedOrder = orderRepository.save(order);
    orderStatusChangedEventWriter.append(savedOrder);
    return savedOrder;
  }

  public OrderResult completePayment(Long orderId) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);
    order.paymentSuccess();
    return OrderResult.from(order);
  }

  @Transactional
  public Order cancelOrder(Long memberId, Long orderId) {
    Order order = orderRepository.findWithLockByIdAndIsDeletedFalse(orderId);
    order.cancel(memberId);
    orderStatusChangedEventWriter.append(order);
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
  public OrderReceptionResult acceptOrder(Long orderId) {
    Order order = orderRepository.findWithLockByIdAndIsDeletedFalse(orderId);
    String pickupCode = generatePickupCode(order.getStoreId());
    order.issuePickupCode(pickupCode);
    orderStatusChangedEventWriter.append(order);
    return OrderReceptionResult.from(order);
  }

  @Transactional
  public PickupStatusResult updatePickupStatus(Long orderId, UpdatePickupStatusCommand command) {
    Order order = orderRepository.findWithLockByIdAndIsDeletedFalse(orderId);

    switch (command.status()) {
      case PICKED_UP -> order.completePickup();
      case NO_SHOW -> order.markNoShow();
      default -> throw new BusinessException(CommonErrorCode.INVALID_STATE);
    }
    orderStatusChangedEventWriter.append(order);

    return PickupStatusResult.from(order);
  }

  @Transactional(readOnly = true)
  public OrderResult getEachOrder(Long memberId, Long orderId) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);
    order.validateOwner(memberId);
    return OrderResult.from(order);
  }

  public PickupCodeResult getPickupCode(Long memberId, Long orderId) {
    Order order = orderRepository.findPickupAvailableOrder(orderId, memberId);
    return PickupCodeResult.from(order);
  }

  @Transactional(readOnly = true)
  public Page<OrderResult> getMyOrders(Long memberId, OrderStatus status, Pageable pageable) {
    return orderRepository
        .findAllByMemberIdAndStatus(memberId, status, pageable)
        .map(OrderResult::from);
  }

  @Transactional(readOnly = true)
  public Page<OrderResult> getStoreOrders(Long storeId, OrderStatus status, Pageable pageable) {
    return orderRepository
        .findAllByStoreIdAndStatus(storeId, status, pageable)
        .map(OrderResult::from);
  }
}
