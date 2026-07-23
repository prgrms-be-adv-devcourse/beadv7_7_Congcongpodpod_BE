package kr.lastdish.core.order.application;

import kr.lastdish.core.common.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.presentation.dto.*;
import lombok.RequiredArgsConstructor;
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
    order.updateOrderStatus(request.status());
    return PickupStatusResponse.from(order);
  }
}
