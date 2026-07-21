package kr.lastdish.core.order.application;

import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import kr.lastdish.core.order.presentation.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    public Order createOrder(Long memberId, OrderCreateRequest request) {
        return Order.create(
                memberId,
                request.storeId(),
                request.dishId(),
                //request.memberName(),
                request.phone(),
                request.dishName(),
                request.quantity(),
                request.unitPrice(),
                request.totalPrice(),
                request.pickupStartAt(),
                request.pickupEndAt()
        );

    }

    public OrderResponse completePayment(Long orderId) {
        Order order = orderRepository.findById(orderId);
        order.paymentSuccess();
        return OrderResponse.from(order);
    }
}
