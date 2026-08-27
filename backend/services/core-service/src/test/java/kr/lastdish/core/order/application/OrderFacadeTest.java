package kr.lastdish.core.order.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.cart.application.CartFacade;
import kr.lastdish.core.cart.application.dto.CartOrderSnapshot;
import kr.lastdish.core.deposit.application.DepositFacade;
import kr.lastdish.core.dish.application.DishFacade;
import kr.lastdish.core.order.application.dto.OrderMemberInfo;
import kr.lastdish.core.order.application.dto.OrderReceptionResult;
import kr.lastdish.core.order.application.dto.OrderResult;
import kr.lastdish.core.order.application.dto.PickupStatusResult;
import kr.lastdish.core.order.application.dto.RejectOrderCommand;
import kr.lastdish.core.order.application.dto.UpdatePickupStatusCommand;
import kr.lastdish.core.order.application.event.OrderNotificationEventWriter;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.domain.MemberSnapshot;
import kr.lastdish.core.order.domain.MemberSnapshotRepository;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRejectReason;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.point.application.PointService;
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
  @Mock private CartFacade cartFacade;

  @Mock private DishFacade dishFacade;

  @Mock private DepositFacade depositFacade;

  @Mock private PointService pointService;

  @Mock private OrderRepository orderRepository;

  @Mock private OrderStatusChangedEventWriter orderStatusChangedEventWriter;

  @Mock private OrderNotificationEventWriter orderNotificationEventWriter;

  @Mock private StoreFacade storeFacade;

  @Mock private MemberSnapshotRepository memberSnapshotRepository;

  @InjectMocks private OrderFacade orderFacade;

  @Test
  @DisplayName("주문 직전 검증을 통과하면 잠금 재고 차감과 결제를 진행한다")
  void payAndCreateOrder_success() {
    // given
    Long memberId = 1L;
    Long cartItemId = 1L;

    CartOrderSnapshot cartItem = createCartOrderSnapshot();

    Order order = mock(Order.class);

    when(order.getId()).thenReturn(10L);
    when(order.getDishId()).thenReturn(100L);
    when(order.getStoreId()).thenReturn(1L);
    when(order.getQuantity()).thenReturn(2L);
    when(order.getUsedDeposit()).thenReturn(BigDecimal.valueOf(7_000));

    when(cartFacade.getValidatedOrderSnapshot(memberId, cartItemId, 3L)).thenReturn(cartItem);
    OrderMemberInfo memberInfo = new OrderMemberInfo("김나영", "010-9999-9999");
    LocalDateTime pickupDeadline = LocalDateTime.of(2026, 8, 20, 19, 0);
    stubMemberSnapshot(memberId, memberInfo);
    when(orderService.validatePickupDeadline(eq(cartItem), any())).thenReturn(pickupDeadline);
    when(orderService.createOrder(
            memberId, memberInfo, cartItem, BigDecimal.valueOf(3_000), pickupDeadline))
        .thenReturn(order);

    OrderResult expectedResponse = mock(OrderResult.class);

    when(orderService.completePayment(10L)).thenReturn(expectedResponse);
    when(storeFacade.getStoreOwnerMemberId(1L)).thenReturn(20L);

    // when
    OrderResult response =
        orderFacade.payAndCreateOrder(memberId, cartItemId, 3L, BigDecimal.valueOf(3_000));

    // then
    assertThat(response).isSameAs(expectedResponse);

    InOrder inOrder =
        inOrder(
            memberSnapshotRepository,
            cartFacade,
            orderService,
            dishFacade,
            storeFacade,
            pointService,
            depositFacade,
            orderNotificationEventWriter);

    inOrder.verify(memberSnapshotRepository).findActiveByMemberId(memberId);
    inOrder.verify(cartFacade).getValidatedOrderSnapshot(memberId, cartItemId, 3L);
    inOrder.verify(storeFacade).validateOpen(1L);
    inOrder.verify(orderService).validatePickupDeadline(eq(cartItem), any());
    inOrder
        .verify(orderService)
        .createOrder(memberId, memberInfo, cartItem, BigDecimal.valueOf(3_000), pickupDeadline);

    inOrder.verify(dishFacade).decreaseStock(100L, 2L);

    inOrder.verify(pointService).use(memberId, 10L, BigDecimal.valueOf(3_000));
    inOrder.verify(depositFacade).use(memberId, 10L, BigDecimal.valueOf(7_000));

    inOrder.verify(orderService).completePayment(10L);
    inOrder.verify(cartFacade).removeOrderedItem(memberId, cartItemId);
    inOrder.verify(storeFacade).getStoreOwnerMemberId(1L);
    inOrder.verify(orderNotificationEventWriter).appendCreated(order, 20L);
  }

  @Test
  @DisplayName("요청한 포인트를 사용하고 포인트 사용에 실패하면 이후 결제를 진행하지 않는다")
  void payAndCreateOrder_pointFailure() {
    Long memberId = 1L;
    Long cartItemId = 1L;
    BigDecimal usedPoint = BigDecimal.valueOf(3_000);
    CartOrderSnapshot cartItem = createCartOrderSnapshot();
    OrderMemberInfo memberInfo = new OrderMemberInfo("김나영", "010-9999-9999");
    LocalDateTime pickupDeadline = LocalDateTime.of(2026, 8, 20, 19, 0);
    Order order = mock(Order.class);

    when(order.getId()).thenReturn(10L);
    when(order.getDishId()).thenReturn(100L);
    when(order.getQuantity()).thenReturn(2L);
    stubMemberSnapshot(memberId, memberInfo);
    when(cartFacade.getValidatedOrderSnapshot(memberId, cartItemId, 3L)).thenReturn(cartItem);
    when(orderService.validatePickupDeadline(eq(cartItem), any())).thenReturn(pickupDeadline);
    when(orderService.createOrder(memberId, memberInfo, cartItem, usedPoint, pickupDeadline))
        .thenReturn(order);
    doThrow(new RuntimeException("포인트 잔액이 부족합니다."))
        .when(pointService)
        .use(memberId, 10L, usedPoint);

    assertThatThrownBy(() -> orderFacade.payAndCreateOrder(memberId, cartItemId, 3L, usedPoint))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("포인트 잔액이 부족합니다.");

    verify(dishFacade).decreaseStock(100L, 2L);
    verify(pointService).use(memberId, 10L, usedPoint);
    verifyNoInteractions(depositFacade);
    verify(orderService, never()).completePayment(anyLong());
    verify(cartFacade, never()).removeOrderedItem(anyLong(), anyLong());
  }

  @Test
  @DisplayName("잠금 재고 차감 시 상품이 판매 중이 아니면 결제를 진행하지 않는다")
  void payAndCreateOrder_dishNotAvailable() {
    Long memberId = 1L;
    Long cartItemId = 1L;
    CartOrderSnapshot cartItem = createCartOrderSnapshot();
    OrderMemberInfo memberInfo = new OrderMemberInfo("김나영", "010-9999-9999");

    stubMemberSnapshot(memberId, memberInfo);
    when(cartFacade.getValidatedOrderSnapshot(memberId, cartItemId, 3L)).thenReturn(cartItem);
    LocalDateTime pickupDeadline = LocalDateTime.of(2026, 8, 20, 19, 0);
    when(orderService.validatePickupDeadline(eq(cartItem), any())).thenReturn(pickupDeadline);

    Order order = mock(Order.class);
    when(order.getDishId()).thenReturn(cartItem.dishId());
    when(order.getQuantity()).thenReturn(cartItem.quantity());
    when(orderService.createOrder(memberId, memberInfo, cartItem, BigDecimal.ZERO, pickupDeadline))
        .thenReturn(order);

    doThrow(
            new kr.lastdish.common.api.exception.BusinessException(
                kr.lastdish.core.common.exception.ErrorCode.DISH_NOT_ON_SALE))
        .when(dishFacade)
        .decreaseStock(cartItem.dishId(), cartItem.quantity());

    assertThatThrownBy(
            () -> orderFacade.payAndCreateOrder(memberId, cartItemId, 3L, BigDecimal.ZERO))
        .isInstanceOf(kr.lastdish.common.api.exception.BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(kr.lastdish.core.common.exception.ErrorCode.DISH_NOT_ON_SALE);

    verify(orderService).validatePickupDeadline(eq(cartItem), any());
    verify(orderService)
        .createOrder(memberId, memberInfo, cartItem, BigDecimal.ZERO, pickupDeadline);
    verify(dishFacade).decreaseStock(cartItem.dishId(), cartItem.quantity());
    verifyNoInteractions(pointService, depositFacade);
    verify(orderService, never()).completePayment(anyLong());
    verify(cartFacade, never()).removeOrderedItem(anyLong(), anyLong());
  }

  @Test
  @DisplayName("회원 스냅샷이 없으면 주문을 진행하지 않는다")
  void payAndCreateOrder_memberSnapshotNotFound() {
    Long memberId = 1L;

    when(memberSnapshotRepository.findActiveByMemberId(memberId))
        .thenReturn(java.util.Optional.empty());

    assertThatThrownBy(() -> orderFacade.payAndCreateOrder(memberId, 1L, 3L, BigDecimal.ZERO))
        .isInstanceOf(kr.lastdish.common.api.exception.BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(kr.lastdish.core.common.exception.ErrorCode.ORDER_MEMBER_SNAPSHOT_NOT_FOUND);

    verifyNoInteractions(cartFacade, orderService, dishFacade, depositFacade);
  }

  @Test
  @DisplayName("매장이 영업 중이 아니면 주문과 결제를 진행하지 않는다")
  void payAndCreateOrder_storeClosed() {
    Long memberId = 1L;
    Long cartItemId = 1L;
    CartOrderSnapshot cartItem = createCartOrderSnapshot();
    OrderMemberInfo memberInfo = new OrderMemberInfo("김나영", "010-9999-9999");

    stubMemberSnapshot(memberId, memberInfo);
    when(cartFacade.getValidatedOrderSnapshot(memberId, cartItemId, 3L)).thenReturn(cartItem);
    doThrow(
            new kr.lastdish.common.api.exception.BusinessException(
                kr.lastdish.core.common.exception.ErrorCode.ORDER_STORE_CLOSED))
        .when(storeFacade)
        .validateOpen(cartItem.storeId());

    assertThatThrownBy(
            () -> orderFacade.payAndCreateOrder(memberId, cartItemId, 3L, BigDecimal.ZERO))
        .isInstanceOf(kr.lastdish.common.api.exception.BusinessException.class)
        .hasMessage("매장이 영업 중이 아닙니다.");

    verify(orderService, never()).validatePickupDeadline(any(), any());
    verifyNoInteractions(orderService, depositFacade);
    verify(cartFacade, never()).removeOrderedItem(anyLong(), anyLong());
  }

  @Test
  @DisplayName("픽업 마감 시간이 지났으면 주문과 결제를 진행하지 않는다")
  void payAndCreateOrder_pickupDeadlinePassed() {
    Long memberId = 1L;
    Long cartItemId = 1L;
    CartOrderSnapshot cartItem = createCartOrderSnapshot();
    OrderMemberInfo memberInfo = new OrderMemberInfo("김나영", "010-9999-9999");

    stubMemberSnapshot(memberId, memberInfo);
    when(cartFacade.getValidatedOrderSnapshot(memberId, cartItemId, 3L)).thenReturn(cartItem);
    doThrow(
            new kr.lastdish.common.api.exception.BusinessException(
                kr.lastdish.core.common.exception.ErrorCode.ORDER_PICKUP_DEADLINE_PASSED))
        .when(orderService)
        .validatePickupDeadline(eq(cartItem), any());

    assertThatThrownBy(
            () -> orderFacade.payAndCreateOrder(memberId, cartItemId, 3L, BigDecimal.ZERO))
        .isInstanceOf(kr.lastdish.common.api.exception.BusinessException.class)
        .hasMessage("상품의 픽업 마감 시간이 지났습니다.");

    verify(storeFacade).validateOpen(cartItem.storeId());
    verify(orderService, never()).createOrder(anyLong(), any(), any(), any(), any());
    verifyNoInteractions(depositFacade);
    verify(cartFacade, never()).removeOrderedItem(anyLong(), anyLong());
  }

  @Test
  @DisplayName("프론트의 가격 버전과 장바구니에 적용된 가격 버전이 다르면 주문하지 않는다")
  void payAndCreateOrder_dishPriceChanged() {
    Long memberId = 1L;
    Long cartItemId = 1L;
    CartOrderSnapshot cartItem = createCartOrderSnapshot();
    OrderMemberInfo memberInfo = new OrderMemberInfo("김나영", "010-9999-9999");

    stubMemberSnapshot(memberId, memberInfo);
    when(cartFacade.getValidatedOrderSnapshot(memberId, cartItemId, 2L))
        .thenThrow(
            new kr.lastdish.common.api.exception.BusinessException(
                kr.lastdish.core.common.exception.ErrorCode.ORDER_DISH_PRICE_CHANGED));

    assertThatThrownBy(
            () -> orderFacade.payAndCreateOrder(memberId, cartItemId, 2L, BigDecimal.ZERO))
        .isInstanceOf(kr.lastdish.common.api.exception.BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(kr.lastdish.core.common.exception.ErrorCode.ORDER_DISH_PRICE_CHANGED);

    verify(orderService, never()).createOrder(anyLong(), any(), any(), any(), any());
    verify(dishFacade, never()).decreaseStock(anyLong(), anyLong());
    verifyNoInteractions(depositFacade);
  }

  private CartOrderSnapshot createCartOrderSnapshot() {
    return new CartOrderSnapshot(
        1L,
        100L,
        "김밥",
        2L,
        BigDecimal.valueOf(6_000),
        BigDecimal.valueOf(5_000),
        LocalTime.of(18, 0),
        LocalTime.of(19, 0));
  }

  @Test
  @DisplayName("예치금 사용에 실패하면 예외를 그대로 전달한다")
  void payAndCreateOrder_depositFailure() {
    // given
    Long memberId = 1L;
    Long cartItemId = 1L;
    CartOrderSnapshot cartItem = createCartOrderSnapshot();

    Order order = mock(Order.class);

    when(order.getId()).thenReturn(10L);
    when(order.getDishId()).thenReturn(100L);
    when(order.getQuantity()).thenReturn(2L);
    when(order.getUsedDeposit()).thenReturn(BigDecimal.valueOf(10_000));

    when(cartFacade.getValidatedOrderSnapshot(memberId, cartItemId, 3L)).thenReturn(cartItem);
    OrderMemberInfo memberInfo = new OrderMemberInfo("김나영", "010-9999-9999");
    LocalDateTime pickupDeadline = LocalDateTime.of(2026, 8, 20, 19, 0);
    stubMemberSnapshot(memberId, memberInfo);
    when(orderService.validatePickupDeadline(eq(cartItem), any())).thenReturn(pickupDeadline);
    when(orderService.createOrder(memberId, memberInfo, cartItem, BigDecimal.ZERO, pickupDeadline))
        .thenReturn(order);

    doThrow(new RuntimeException("예치금 잔액이 부족합니다."))
        .when(depositFacade)
        .use(memberId, 10L, BigDecimal.valueOf(10_000));

    // when & then
    assertThatThrownBy(
            () -> orderFacade.payAndCreateOrder(memberId, cartItemId, 3L, BigDecimal.ZERO))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("예치금 잔액이 부족합니다.");

    verify(dishFacade).decreaseStock(100L, 2L);

    verify(depositFacade).use(memberId, 10L, BigDecimal.valueOf(10_000));
    verify(cartFacade, never()).removeOrderedItem(anyLong(), anyLong());
    verify(orderNotificationEventWriter, never()).appendCreated(any(), anyLong());
  }

  @Test
  @DisplayName("판매자가 주문을 접수하면 매장 소유자를 검증하고 픽업 코드를 발급한다")
  void acceptOrder_success() {
    Long memberId = 1L;
    Long orderId = 10L;
    Long storeId = 100L;
    Order order = mock(Order.class);
    OrderReceptionResult expectedResponse = mock(OrderReceptionResult.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(orderService.acceptOrder(orderId)).thenReturn(expectedResponse);

    OrderReceptionResult response = orderFacade.acceptOrder(memberId, "SELLER", orderId);

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

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(orderRepository.findWithLockByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(order.getMemberId()).thenReturn(customerId);
    when(order.getDishId()).thenReturn(dishId);
    when(order.getQuantity()).thenReturn(quantity);
    when(order.getTotalPrice()).thenReturn(totalPrice);

    orderFacade.rejectOrder(sellerId, "SELLER", orderId, new RejectOrderCommand(reason));

    verify(storeFacade).validateStoreOwner(storeId, sellerId);
    verify(order).rejectOrder(reason);
    verify(orderStatusChangedEventWriter).append(order);
    verify(orderNotificationEventWriter).appendRejected(order, reason);
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

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(orderRepository.findWithLockByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(order.getMemberId()).thenReturn(customerId);
    when(order.getTotalPrice()).thenReturn(totalPrice);

    orderFacade.rejectOrder(sellerId, "SELLER", orderId, new RejectOrderCommand(reason));

    verify(storeFacade).validateStoreOwner(storeId, sellerId);
    verify(order).rejectOrder(reason);
    verify(orderStatusChangedEventWriter).append(order);
    verify(orderNotificationEventWriter).appendRejected(order, reason);
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
    UpdatePickupStatusCommand command = new UpdatePickupStatusCommand(OrderStatus.PICKED_UP);
    PickupStatusResult expectedResponse = mock(PickupStatusResult.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(orderService.updatePickupStatus(orderId, command)).thenReturn(expectedResponse);

    PickupStatusResult response = orderFacade.updateOrder(memberId, "SELLER", orderId, command);

    assertThat(response).isSameAs(expectedResponse);

    InOrder inOrder = inOrder(orderRepository, storeFacade, orderService);
    inOrder.verify(orderRepository).findByIdAndIsDeletedFalse(orderId);
    inOrder.verify(storeFacade).validateStoreOwner(storeId, memberId);
    inOrder.verify(orderService).updatePickupStatus(orderId, command);
  }

  @Test
  @DisplayName("판매자 권한이 아니면 픽업 상태를 변경하지 않는다")
  void updateOrder_notSeller() {
    Long memberId = 1L;
    Long orderId = 10L;
    UpdatePickupStatusCommand command = new UpdatePickupStatusCommand(OrderStatus.PICKED_UP);

    assertThatThrownBy(() -> orderFacade.updateOrder(memberId, "MEMBER", orderId, command))
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
    OrderResult orderResult = mock(OrderResult.class);
    Page<OrderResult> expected = new PageImpl<>(List.of(orderResult), pageable, 1);

    when(orderService.getStoreOrders(storeId, status, pageable)).thenReturn(expected);

    Page<OrderResult> response =
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

  private void stubMemberSnapshot(Long memberId, OrderMemberInfo memberInfo) {
    when(memberSnapshotRepository.findActiveByMemberId(memberId))
        .thenReturn(
            java.util.Optional.of(
                MemberSnapshot.create(memberId, memberInfo.name(), memberInfo.phone())));
  }
}
