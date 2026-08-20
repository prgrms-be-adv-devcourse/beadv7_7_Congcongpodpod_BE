package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.cart.application.dto.CartOrderSnapshot;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.order.application.dto.*;
import kr.lastdish.core.order.application.event.OrderNoShowEventWriter;
import kr.lastdish.core.order.application.event.OrderPickedUpEventWriter;
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
  private final OrderPickedUpEventWriter orderPickedUpEventWriter;
  private final OrderNoShowEventWriter orderNoShowEventWriter;
  private final PickupCodeGenerator pickupCodeGenerator;
  private static final int MAX_PICKUP_CODE_RETRY = 5;

  // 장바구니 스냅샷의 정가·판매가로 주문을 만든다. 절약 금액은 Order가 두 값에서 계산한다.
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
            cartItem.dishPrice(),
            cartItem.unitPrice(),
            cartItem.pickupStartAt(),
            cartItem.pickupEndAt(),
            pickupDeadline(cartItem));

    Order savedOrder = orderRepository.save(order);
    orderStatusChangedEventWriter.append(savedOrder);
    return savedOrder;
  }

  private LocalDateTime pickupDeadline(CartOrderSnapshot cartItem) {
    LocalDateTime now = LocalDateTime.now();
    var pickupDate = now.toLocalDate();

    boolean crossesMidnight = cartItem.pickupEndAt().isBefore(cartItem.pickupStartAt());
    boolean beforePickupDayStarts = now.toLocalTime().isAfter(cartItem.pickupEndAt());

    if (crossesMidnight && beforePickupDayStarts) {
      pickupDate = pickupDate.plusDays(1);
    }

    return pickupDate.atTime(cartItem.pickupEndAt());
  }

  /** 픽업 마감 일시가 지난 경우에만 주문을 중단한다. */
  public void validatePickupDeadline(CartOrderSnapshot cartItem) {
    LocalDateTime now = LocalDateTime.now();
    if (now.isAfter(pickupDeadline(cartItem))) {
      throw new BusinessException(ErrorCode.ORDER_PICKUP_DEADLINE_PASSED);
    }
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
      case PICKED_UP -> {
        order.completePickup(LocalDateTime.now());
      }
      case NO_SHOW -> {
        order.markNoShow(LocalDateTime.now());
      }
      default -> throw new BusinessException(CommonErrorCode.INVALID_STATE);
    }

    long aggregateVersion = order.nextEventVersion();
    orderStatusChangedEventWriter.append(order, aggregateVersion);

    if (command.status() == OrderStatus.PICKED_UP) {
      // TODO: 정산·포인트 Consumer 준비 후 ORDER_PICKED_UP Outbox 이벤트 발행 활성화
      // orderPickedUpEventWriter.append(order, aggregateVersion);
    } else if (command.status() == OrderStatus.NO_SHOW) {
      // TODO: 정산 Consumer 준비 후 ORDER_NO_SHOW Outbox 이벤트 발행 활성화
      // orderNoShowEventWriter.append(order, aggregateVersion);
    }

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
