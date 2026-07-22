package kr.lastdish.core.order.application;

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

  @Transactional
  // 주문 접수 - 픽업 코드 발급
  public OrderReceptionResponse acceptOrder(Long orderId) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);

    // 중복일시 다시 생성
    String pickupCode;
    do {
      pickupCode = pickupCodeGenerator.generate();
    } while (orderRepository.validateActivePickUpCode(order.getStoreId(), pickupCode));

    order.issuePickupCode(pickupCode);
    return OrderReceptionResponse.from(order);
  }
}
