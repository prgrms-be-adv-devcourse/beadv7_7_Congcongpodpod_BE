package kr.lastdish.core.order.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.dish.application.DishFacade;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRejectReason;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import kr.lastdish.core.order.presentation.dto.OrderReceptionResponse;
import kr.lastdish.core.order.presentation.dto.OrderRejectRequest;
import kr.lastdish.core.order.presentation.dto.OrderResponse;
import kr.lastdish.core.order.presentation.dto.PickupStatusRequest;
import kr.lastdish.core.order.presentation.dto.PickupStatusResponse;
import kr.lastdish.core.payment.application.DepositFacade;
import kr.lastdish.core.store.application.StoreFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

  @Mock private OrderService orderService;

  @Mock private DishFacade dishFacade;

  @Mock private DepositFacade depositFacade;

  @Mock private OrderRepository orderRepository;

  @Mock private StoreFacade storeFacade;

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

    InOrder inOrder = inOrder(orderService, dishFacade, depositFacade);

    inOrder.verify(orderService).createOrder(memberId, request);

    inOrder.verify(dishFacade).decreaseStock(100L, 2L);

    inOrder.verify(depositFacade).use(memberId, 10L, BigDecimal.valueOf(10_000));

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
        .when(depositFacade)
        .use(memberId, 10L, BigDecimal.valueOf(10_000));

    // when & then
    assertThatThrownBy(() -> orderFacade.payAndCreateOrder(memberId, request))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("예치금 잔액이 부족합니다.");

    verify(dishFacade).decreaseStock(100L, 2L);

    verify(depositFacade).use(memberId, 10L, BigDecimal.valueOf(10_000));
  }

  @Test
  @DisplayName("판매자가 주문을 접수하면 매장 소유자를 검증하고 픽업 코드를 발급한다")
  void acceptOrder_success() {
    Long memberId = 1L;
    Long orderId = 10L;
    Long storeId = 100L;
    Order order = mock(Order.class);
    OrderReceptionResponse expectedResponse = mock(OrderReceptionResponse.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(orderService.acceptOrder(orderId)).thenReturn(expectedResponse);

    OrderReceptionResponse response = orderFacade.acceptOrder(memberId, "SELLER", orderId);

    assertThat(response).isSameAs(expectedResponse);

    InOrder inOrder = inOrder(orderRepository, storeFacade, orderService);
    inOrder.verify(orderRepository).findByIdAndIsDeletedFalse(orderId);
    inOrder.verify(storeFacade).validateStoreOwner(storeId, memberId);
    inOrder.verify(orderService).acceptOrder(orderId);
  }

  @Test
  @DisplayName("판매자 권한이 아니면 주문 접수를 처리하지 않는다")
  void acceptOrder_notSeller() {
    Long memberId = 1L;
    Long orderId = 10L;

    assertThatThrownBy(() -> orderFacade.acceptOrder(memberId, "MEMBER", orderId))
        .isInstanceOf(RuntimeException.class);

    verifyNoInteractions(orderRepository, storeFacade, orderService);
  }

  @ParameterizedTest(name = "{0} 사유이면 재고를 복구한다")
  @EnumSource(
      value = OrderRejectReason.class,
      names = {"NOT_READY", "SYSTEM_ERROR"})
  @DisplayName("재고 복구가 필요한 모든 반려 사유는 환불과 재고 복구를 처리한다")
  void rejectOrder_restoreStock(OrderRejectReason reason) {
    Long sellerId = 1L;
    Long customerId = 2L;
    Long orderId = 10L;
    Long storeId = 100L;
    Long dishId = 200L;
    Long quantity = 2L;
    BigDecimal totalPrice = BigDecimal.valueOf(10_000);
    Order order = mock(Order.class);
    OrderRejectRequest request = new OrderRejectRequest(reason);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(orderRepository.findWithLockByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(order.getMemberId()).thenReturn(customerId);
    when(order.getDishId()).thenReturn(dishId);
    when(order.getQuantity()).thenReturn(quantity);
    when(order.getTotalPrice()).thenReturn(totalPrice);

    orderFacade.rejectOrder(sellerId, "SELLER", orderId, request);

    verify(storeFacade).validateStoreOwner(storeId, sellerId);
    verify(order).rejectOrder(reason);
    verify(depositFacade).refund(customerId, orderId, totalPrice);
    verify(dishFacade).increaseStock(dishId, quantity);
  }

  @ParameterizedTest(name = "{0} 사유이면 재고를 복구하지 않는다")
  @EnumSource(
      value = OrderRejectReason.class,
      names = {"OUT_OF_STOCK", "QUALITY_ISSUE", "STORE_CLOSED"})
  @DisplayName("재고 복구가 필요하지 않은 모든 반려 사유는 환불만 처리한다")
  void rejectOrder_withoutStockRestore(OrderRejectReason reason) {
    Long sellerId = 1L;
    Long customerId = 2L;
    Long orderId = 10L;
    Long storeId = 100L;
    BigDecimal totalPrice = BigDecimal.valueOf(10_000);
    Order order = mock(Order.class);
    OrderRejectRequest request = new OrderRejectRequest(reason);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(orderRepository.findWithLockByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(order.getMemberId()).thenReturn(customerId);
    when(order.getTotalPrice()).thenReturn(totalPrice);

    orderFacade.rejectOrder(sellerId, "SELLER", orderId, request);

    verify(storeFacade).validateStoreOwner(storeId, sellerId);
    verify(order).rejectOrder(reason);
    verify(depositFacade).refund(customerId, orderId, totalPrice);
    verify(dishFacade, never()).increaseStock(anyLong(), anyLong());
  }

  @Test
  @DisplayName("판매자가 픽업 상태를 변경하면 매장 소유자를 검증하고 상태를 업데이트한다")
  void updateOrder_success() {
    Long memberId = 1L;
    Long orderId = 10L;
    Long storeId = 100L;
    Order order = mock(Order.class);
    PickupStatusRequest request = mock(PickupStatusRequest.class);
    PickupStatusResponse expectedResponse = mock(PickupStatusResponse.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(orderService.updatePickupStatus(orderId, request)).thenReturn(expectedResponse);

    PickupStatusResponse response = orderFacade.updateOrder(memberId, "SELLER", orderId, request);

    assertThat(response).isSameAs(expectedResponse);

    InOrder inOrder = inOrder(orderRepository, storeFacade, orderService);
    inOrder.verify(orderRepository).findByIdAndIsDeletedFalse(orderId);
    inOrder.verify(storeFacade).validateStoreOwner(storeId, memberId);
    inOrder.verify(orderService).updatePickupStatus(orderId, request);
  }

  @Test
  @DisplayName("판매자 권한이 아니면 픽업 상태를 변경하지 않는다")
  void updateOrder_notSeller() {
    Long memberId = 1L;
    Long orderId = 10L;
    PickupStatusRequest request = mock(PickupStatusRequest.class);

    assertThatThrownBy(() -> orderFacade.updateOrder(memberId, "MEMBER", orderId, request))
        .isInstanceOf(RuntimeException.class);

    verifyNoInteractions(orderRepository, storeFacade, orderService);
  }

  @Test
  @DisplayName("판매자가 자신의 매장 주문 목록을 조회한다")
  void getStoreOrders_success() {
    Long memberId = 1L;
    Long storeId = 100L;
    OrderStatus status = OrderStatus.PICKUP_READY;
    Pageable pageable = PageRequest.of(0, 20);
    OrderResponse orderResponse = mock(OrderResponse.class);
    Page<OrderResponse> expected = new PageImpl<>(List.of(orderResponse), pageable, 1);

    when(orderService.getStoreOrders(storeId, status, pageable)).thenReturn(expected);

    Page<OrderResponse> response =
        orderFacade.getStoreOrders(memberId, "SELLER", storeId, status, pageable);

    assertThat(response).isSameAs(expected);
    assertThat(response.getTotalElements()).isEqualTo(1);

    InOrder inOrder = inOrder(storeFacade, orderService);
    inOrder.verify(storeFacade).validateStoreOwner(storeId, memberId);
    inOrder.verify(orderService).getStoreOrders(storeId, status, pageable);
  }

  @Test
  @DisplayName("판매자 권한이 아니면 매장 주문 목록을 조회하지 않는다")
  void getStoreOrders_notSeller() {
    Long memberId = 1L;
    Long storeId = 100L;
    Pageable pageable = PageRequest.of(0, 20);

    assertThatThrownBy(
            () -> orderFacade.getStoreOrders(memberId, "MEMBER", storeId, null, pageable))
        .isInstanceOf(RuntimeException.class);

    verifyNoInteractions(storeFacade, orderService);
  }

  @Test
  @DisplayName("매장 소유자가 아니면 주문 목록을 조회하지 않는다")
  void getStoreOrders_notStoreOwner() {
    Long memberId = 1L;
    Long storeId = 100L;
    Pageable pageable = PageRequest.of(0, 20);
    RuntimeException exception = new RuntimeException("매장 소유자가 아닙니다.");

    doThrow(exception).when(storeFacade).validateStoreOwner(storeId, memberId);

    assertThatThrownBy(
            () -> orderFacade.getStoreOrders(memberId, "SELLER", storeId, null, pageable))
        .isSameAs(exception);

    verify(storeFacade).validateStoreOwner(storeId, memberId);
    verify(orderService, never()).getStoreOrders(anyLong(), any(), any(Pageable.class));
  }
}
