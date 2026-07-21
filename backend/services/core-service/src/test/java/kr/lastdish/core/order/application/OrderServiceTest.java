package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.presentation.dto.OrderCancelRequest;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @InjectMocks private OrderService orderService;

  @Test
  void createOrder_success() {
    Long memberId = 1L;
    OrderCreateRequest request =
        new OrderCreateRequest(
            2L, // storeId
            3L, // dishId
            "010-1234-5678", // phone
            "DishName",
            4L, // quantity
            BigDecimal.valueOf(5000),
            LocalTime.of(18, 0),
            LocalTime.of(19, 0));

    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Order order = orderService.createOrder(memberId, request);

    assertThat(order).isNotNull();
    assertThat(order.getMemberId()).isEqualTo(memberId);
    assertThat(order.getStoreId()).isEqualTo(request.storeId());
    assertThat(order.getDishId()).isEqualTo(request.dishId());
    assertThat(order.getQuantity()).isEqualTo(request.quantity());
    assertThat(order.getPhone()).isEqualTo(request.phone());
    assertThat(order.getDishName()).isEqualTo(request.dishName());
    assertThat(order.getUnitPrice()).isEqualByComparingTo(request.unitPrice());
    assertThat(order.getTotalPrice())
        .isEqualByComparingTo(request.unitPrice().multiply(BigDecimal.valueOf(request.quantity())));

    verify(orderRepository, times(1)).save(any(Order.class));
  }

  @Test
  void completePayment_success() {
    Long orderId = 1L;
    Order order = mock(Order.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);

    orderService.completePayment(orderId);

    verify(orderRepository, times(1)).findByIdAndIsDeletedFalse(orderId);
    verify(order, times(1)).paymentSuccess();
  }

  @Test
  void cancelOrder_success() {
    Long memberId = 1L;
    Long orderId = 2L;
    Order order = mock(Order.class);
    OrderCancelRequest request = new OrderCancelRequest("Change of plans");

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);

    Order result = orderService.cancelOrder(memberId, orderId, request);

    assertThat(result).isSameAs(order);
    verify(orderRepository, times(1)).findByIdAndIsDeletedFalse(orderId);
    verify(order, times(1)).cancel(memberId, request.cancelReason());
  }
}
