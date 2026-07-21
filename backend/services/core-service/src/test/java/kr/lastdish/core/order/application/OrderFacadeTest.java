package kr.lastdish.core.order.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import kr.lastdish.core.dish.application.DishService;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import kr.lastdish.core.order.presentation.dto.OrderResponse;
import kr.lastdish.core.payment.application.DepositService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

  @Mock private OrderService orderService;

  @Mock private DishService dishService;

  @Mock private DepositService depositService;

  @InjectMocks private OrderFacade orderFacade;

  @Test
  @DisplayName("주문 생성 시 재고를 차감하고 예치금을 사용한다")
  void payAndCreateOrder_success() {
    // given
    Long memberId = 1L;

    OrderCreateRequest request = createRequest();

    Order order = mock(Order.class);

    when(order.getId()).thenReturn(10L);
    when(order.getDishId()).thenReturn(100L);
    when(order.getQuantity()).thenReturn(2L);
    when(order.getTotalPrice()).thenReturn(BigDecimal.valueOf(10_000));

    when(orderService.createOrder(memberId, request)).thenReturn(order);

    OrderResponse expectedResponse = mock(OrderResponse.class);

    when(orderService.completePayment(10L)).thenReturn(expectedResponse);

    // when
    OrderResponse response = orderFacade.payAndCreateOrder(memberId, request);

    // then
    assertThat(response).isSameAs(expectedResponse);

    InOrder inOrder = inOrder(orderService, dishService, depositService);

    inOrder.verify(orderService).createOrder(memberId, request);

    inOrder.verify(dishService).decreaseStock(100L, 2L);

    inOrder.verify(depositService).use(memberId, 10L, BigDecimal.valueOf(10_000));

    inOrder.verify(orderService).completePayment(10L);
  }

  private OrderCreateRequest createRequest() {
    return new OrderCreateRequest(
        1L,
        1L,
        "010-1234-5678",
        "김밥",
        2L,
        BigDecimal.valueOf(5_000),
        LocalTime.of(18, 0),
        LocalTime.of(19, 0));
  }

  @Test
  @DisplayName("예치금 사용에 실패하면 예외를 그대로 전달한다")
  void payAndCreateOrder_depositFailure() {
    // given
    Long memberId = 1L;
    OrderCreateRequest request = createRequest();

    Order order = mock(Order.class);

    when(order.getId()).thenReturn(10L);
    when(order.getDishId()).thenReturn(100L);
    when(order.getQuantity()).thenReturn(2L);
    when(order.getTotalPrice()).thenReturn(BigDecimal.valueOf(10_000));

    when(orderService.createOrder(memberId, request)).thenReturn(order);

    doThrow(new RuntimeException("예치금 잔액이 부족합니다."))
        .when(depositService)
        .use(memberId, 10L, BigDecimal.valueOf(10_000));

    // when & then
    assertThatThrownBy(() -> orderFacade.payAndCreateOrder(memberId, request))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("예치금 잔액이 부족합니다.");

    verify(dishService).decreaseStock(100L, 2L);

    verify(depositService).use(memberId, 10L, BigDecimal.valueOf(10_000));
  }
}
