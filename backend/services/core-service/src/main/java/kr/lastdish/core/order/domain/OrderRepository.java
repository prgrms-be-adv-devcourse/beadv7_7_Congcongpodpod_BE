package kr.lastdish.core.order.domain;

public interface OrderRepository {
    Order save(Order order);

    Order findById(Long orderId);
}
