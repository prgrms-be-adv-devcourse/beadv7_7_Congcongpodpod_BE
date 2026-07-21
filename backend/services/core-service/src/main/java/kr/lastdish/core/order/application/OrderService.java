package kr.lastdish.core.order.application;

import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.presentation.dto.OrderCancelRequest;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import kr.lastdish.core.order.presentation.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    public Order createOrder(Long memberId, OrderCreateRequest request) {
        Order order = Order.create(
                memberId,
                request.storeId(),
                request.dishId(),
                //request.memberName(),
                request.phone(),
                request.dishName(),
                request.quantity(),
                request.unitPrice(),
                request.pickupStartAt(),
                request.pickupEndAt()
        );

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
}
