package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.presentation.dto.OrderCancelRequest;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import kr.lastdish.core.order.presentation.dto.OrderResponse;
import kr.lastdish.core.order.presentation.dto.OrderSettlementInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orderRepository;

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

  public Order cancelOrder(Long memberId, Long orderId, OrderCancelRequest request) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);
    order.cancel(memberId, request.cancelReason());
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
}
