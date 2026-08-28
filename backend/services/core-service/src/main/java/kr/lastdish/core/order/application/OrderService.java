package kr.lastdish.core.order.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.cart.application.dto.CartOrderSnapshot;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.order.application.dto.*;
import kr.lastdish.core.order.application.event.OrderNoShowEventWriter;
import kr.lastdish.core.order.application.event.OrderNotificationEventWriter;
import kr.lastdish.core.order.application.event.OrderPickedUpEventWriter;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
  private static final int MAX_PICKUP_CODE_RETRY = 5;
  public static final int PICKUP_EXPIRATION_BATCH_SIZE = 1000;

  private final OrderRepository orderRepository;
  private final OrderStatusChangedEventWriter orderStatusChangedEventWriter;
  private final OrderNotificationEventWriter orderNotificationEventWriter;
  private final OrderPickedUpEventWriter orderPickedUpEventWriter;
  private final OrderNoShowEventWriter orderNoShowEventWriter;
  private final PickupCodeGenerator pickupCodeGenerator;

  // 장바구니 스냅샷의 정가·판매가로 주문을 만든다. 절약 금액은 Order가 두 값에서 계산한다.
  public Order createOrder(
      Long memberId,
      OrderMemberInfo memberInfo,
      CartOrderSnapshot cartItem,
      BigDecimal usedPoint,
      LocalDateTime pickupDeadline) {
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
            usedPoint,
            cartItem.pickupStartAt(),
            cartItem.pickupEndAt(),
            pickupDeadline);

    Order savedOrder = orderRepository.save(order);
    orderStatusChangedEventWriter.append(savedOrder);
    return savedOrder;
  }

  /** 픽업 마감 일시를 한 번 계산해 검증하고 주문 생성에 사용할 값으로 반환한다. */
  public LocalDateTime validatePickupDeadline(CartOrderSnapshot cartItem, LocalDateTime now) {
    LocalDateTime pickupDeadline =
        Order.calculatePickupDeadline(now, cartItem.pickupStartAt(), cartItem.pickupEndAt());
    if (!now.isBefore(pickupDeadline)) {
      throw new BusinessException(ErrorCode.ORDER_PICKUP_DEADLINE_PASSED);
    }

    return pickupDeadline;
  }

  @Transactional
  public int expirePickupOrders(LocalDateTime now) {
    var expirationTargets =
        orderRepository
            .findPickupExpirationTargets(now, PageRequest.of(0, PICKUP_EXPIRATION_BATCH_SIZE))
            .stream()
            .limit(PICKUP_EXPIRATION_BATCH_SIZE)
            .toList();
    expirationTargets.forEach(
        order -> {
          order.markNoShow(now);
          long aggregateVersion = order.nextEventVersion();
          orderStatusChangedEventWriter.append(order, aggregateVersion);
          orderNoShowEventWriter.append(order, aggregateVersion);
        });
    return expirationTargets.size();
  }

  @Transactional(readOnly = true)
  public boolean hasActiveOrdersForDish(Long dishId) {
    return orderRepository.existsActiveOrderByDishId(dishId);
  }

  // 결제 성공을 주문에 반영한다. 호출부가 방금 만든 주문 엔티티를 그대로 넘긴다 —
  // ID로 다시 찾으면 파생 쿼리라 1차 캐시를 타지 않고 SELECT가 한 번 더 나간다(2026-08-28 실측).
  public OrderResult completePayment(Order order) {
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

    return orderRepository
        .findSettlementTargetOrders(storeId, SETTLEMENT_TARGET_STATUSES, periodStart, periodEnd)
        .stream()
        .map(OrderSettlementInfo::from)
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
    orderNotificationEventWriter.appendAccepted(order);
    return OrderReceptionResult.from(order);
  }

  @Transactional
  public PickupStatusResult updatePickupStatus(Long orderId, UpdatePickupStatusCommand command) {
    Order order = orderRepository.findWithLockByIdAndIsDeletedFalse(orderId);

    switch (command.status()) {
      case PICKED_UP -> {
        order.completePickup(LocalDateTime.now(BUSINESS_ZONE));
      }
      case NO_SHOW -> {
        order.markNoShow(LocalDateTime.now(BUSINESS_ZONE));
      }
      default -> throw new BusinessException(CommonErrorCode.INVALID_STATE);
    }

    long aggregateVersion = order.nextEventVersion();
    orderStatusChangedEventWriter.append(order, aggregateVersion);

    if (command.status() == OrderStatus.PICKED_UP) {
      orderPickedUpEventWriter.append(order, aggregateVersion);
      orderNotificationEventWriter.appendPickedUp(order);
    } else if (command.status() == OrderStatus.NO_SHOW) {
      orderNoShowEventWriter.append(order, aggregateVersion);
      orderNotificationEventWriter.appendNoShow(order);
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

  @Transactional(readOnly = true)
  public boolean existsNotCompletedOrder(Long storeId) {
    return orderRepository.existsNotCompletedOrder(storeId);
  }
}
