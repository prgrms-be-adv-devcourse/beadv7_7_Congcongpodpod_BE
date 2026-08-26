package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.cart.application.dto.CartOrderSnapshot;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.order.application.dto.OrderMemberInfo;
import kr.lastdish.core.order.application.dto.OrderReceptionResult;
import kr.lastdish.core.order.application.dto.OrderResult;
import kr.lastdish.core.order.application.dto.PickupStatusResult;
import kr.lastdish.core.order.application.dto.UpdatePickupStatusCommand;
import kr.lastdish.core.order.application.event.OrderNoShowEventWriter;
import kr.lastdish.core.order.application.event.OrderNotificationEventWriter;
import kr.lastdish.core.order.application.event.OrderPickedUpEventWriter;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.store.application.StoreService;
import kr.lastdish.core.store.application.dto.StoreResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private OrderStatusChangedEventWriter orderStatusChangedEventWriter;

  @Mock private OrderNotificationEventWriter orderNotificationEventWriter;

  @Mock private OrderPickedUpEventWriter orderPickedUpEventWriter;

  @Mock private OrderNoShowEventWriter orderNoShowEventWriter;

  @Mock private PickupCodeGenerator pickupCodeGenerator;

  private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 8, 19, 12, 0);

  private OrderService orderService;

  @BeforeEach
  void setUp() {
    orderService =
        new OrderService(
            orderRepository,
            orderStatusChangedEventWriter,
            orderNotificationEventWriter,
            orderPickedUpEventWriter,
            orderNoShowEventWriter,
            pickupCodeGenerator);
  }

  @Test
  void dish_픽업_마감_시간이_지난_주문을_노쇼_처리한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 19, 0);
    Order firstOrder = mock(Order.class);
    Order secondOrder = mock(Order.class);
    when(orderRepository.findPickupExpirationTargets(eq(now), any(Pageable.class)))
        .thenReturn(List.of(firstOrder, secondOrder));
    when(firstOrder.nextEventVersion()).thenReturn(3L);
    when(secondOrder.nextEventVersion()).thenReturn(5L);

    assertThat(orderService.expirePickupOrders(now)).isEqualTo(2);

    verify(firstOrder).markNoShow(now);
    verify(secondOrder).markNoShow(now);
    verify(orderStatusChangedEventWriter).append(firstOrder, 3L);
    verify(orderStatusChangedEventWriter).append(secondOrder, 5L);
    verify(orderNoShowEventWriter).append(firstOrder, 3L);
    verify(orderNoShowEventWriter).append(secondOrder, 5L);
  }

  @Test
  void 픽업_만료_주문은_한_트랜잭션에서_1000건까지_처리한다() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 10, 19, 0);
    List<Order> orders = IntStream.range(0, 1001).mapToObj(index -> mock(Order.class)).toList();
    when(orderRepository.findPickupExpirationTargets(eq(now), any(Pageable.class)))
        .thenReturn(orders);

    assertThat(orderService.expirePickupOrders(now)).isEqualTo(1000);

    verify(orderRepository).findPickupExpirationTargets(now, PageRequest.of(0, 1000));
    verify(orders.get(999)).markNoShow(now);
    verify(orders.get(1000), never()).markNoShow(now);
  }

  @Test
  void Dish의_진행중_주문_존재_여부를_조회한다() {
    when(orderRepository.existsActiveOrderByDishId(10L)).thenReturn(true);

    assertThat(orderService.hasActiveOrdersForDish(10L)).isTrue();

    verify(orderRepository).existsActiveOrderByDishId(10L);
  }

  @Test
  @DisplayName("장바구니 스냅샷과 회원 정보로 주문을 생성한다")
  void createOrder_success() {
    Long memberId = 1L;
    OrderMemberInfo memberInfo = new OrderMemberInfo("테스트 회원", "010-1234-5678");
    CartOrderSnapshot cartItem =
        new CartOrderSnapshot(
            2L,
            3L,
            "DishName",
            4L,
            BigDecimal.valueOf(6000),
            BigDecimal.valueOf(5000),
            LocalTime.of(18, 0),
            LocalTime.of(19, 0));

    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    Order order =
        orderService.createOrder(
            memberId, memberInfo, cartItem, FIXED_NOW.toLocalDate().atTime(cartItem.pickupEndAt()));

    assertThat(order).isNotNull();
    assertThat(order.getMemberId()).isEqualTo(memberId);
    assertThat(order.getStoreId()).isEqualTo(2L);
    assertThat(order.getDishId()).isEqualTo(3L);
    assertThat(order.getQuantity()).isEqualTo(4L);
    assertThat(order.getMemberName()).isEqualTo(memberInfo.name());
    assertThat(order.getPhone()).isEqualTo(memberInfo.phone());
    assertThat(order.getDishName()).isEqualTo("DishName");
    assertThat(order.getUnitPrice()).isEqualByComparingTo("5000");
    assertThat(order.getTotalPrice()).isEqualByComparingTo("20000");
    assertThat(order.getTotalSavedAmount()).isEqualByComparingTo("4000");
    assertThat(order.getPickupDeadline())
        .isEqualTo(FIXED_NOW.toLocalDate().atTime(cartItem.pickupEndAt()));

    verify(orderRepository, times(1)).save(any(Order.class));
    verify(orderStatusChangedEventWriter).append(order);
  }

  @ParameterizedTest(name = "현재 {0}이면 픽업 마감은 {1}")
  @MethodSource("crossMidnightDeadlineCases")
  @DisplayName("자정을 넘기는 픽업의 마감 일시를 계산한다")
  void 자정을_넘기는_픽업의_마감_일시를_계산한다(LocalDateTime now, LocalDateTime expectedDeadline) {
    CartOrderSnapshot cartItem = createCartOrderSnapshot(LocalTime.of(23, 0), LocalTime.of(1, 0));
    LocalDateTime pickupDeadline = orderService.validatePickupDeadline(cartItem, now);

    assertThat(pickupDeadline).isEqualTo(expectedDeadline);
  }

  private static Stream<Arguments> crossMidnightDeadlineCases() {
    return Stream.of(
        Arguments.of(LocalDateTime.of(2026, 8, 19, 0, 30), LocalDateTime.of(2026, 8, 19, 1, 0)),
        Arguments.of(LocalDateTime.of(2026, 8, 20, 0, 30), LocalDateTime.of(2026, 8, 20, 1, 0)));
  }

  @ParameterizedTest(name = "현재 {0}, 픽업 {1}~{2}이면 주문 가능")
  @MethodSource("orderablePickupDeadlineCases")
  @DisplayName("픽업 마감 전이면 주문할 수 있다")
  void 픽업_마감_전이면_주문할_수_있다(LocalDateTime now, LocalTime pickupStartAt, LocalTime pickupEndAt) {
    CartOrderSnapshot cartItem = createCartOrderSnapshot(pickupStartAt, pickupEndAt);
    orderService.validatePickupDeadline(cartItem, now);
  }

  private static Stream<Arguments> orderablePickupDeadlineCases() {
    return Stream.of(
        Arguments.of(
            LocalDateTime.of(2026, 8, 19, 12, 0), LocalTime.of(11, 0), LocalTime.of(13, 0)),
        Arguments.of(
            LocalDateTime.of(2026, 8, 19, 12, 0), LocalTime.of(18, 0), LocalTime.of(19, 0)),
        Arguments.of(
            LocalDateTime.of(2026, 8, 20, 0, 30), LocalTime.of(23, 0), LocalTime.of(1, 0)));
  }

  @Test
  @DisplayName("정확히 픽업 마감 시각이면 주문할 수 없다")
  void 정확히_픽업_마감_시각이면_주문할_수_없다() {
    CartOrderSnapshot cartItem = createCartOrderSnapshot(LocalTime.of(18, 0), LocalTime.of(19, 0));
    LocalDateTime now = LocalDateTime.of(2026, 8, 19, 19, 0);
    assertThatThrownBy(() -> orderService.validatePickupDeadline(cartItem, now))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.ORDER_PICKUP_DEADLINE_PASSED);
  }

  private CartOrderSnapshot createCartOrderSnapshot(
      LocalTime pickupStartAt, LocalTime pickupEndAt) {
    return new CartOrderSnapshot(
        2L,
        3L,
        "DishName",
        1L,
        BigDecimal.valueOf(6000),
        BigDecimal.valueOf(5000),
        pickupStartAt,
        pickupEndAt);
  }

  @Test
  @DisplayName("결제 대기 주문을 결제 완료로 변경한다")
  void completePayment_success() {
    Long orderId = 1L;
    Order order = mock(Order.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);

    orderService.completePayment(orderId);

    verify(orderRepository, times(1)).findByIdAndIsDeletedFalse(orderId);
    verify(order, times(1)).paymentSuccess();
  }

  @Test
  @DisplayName("주문을 취소한다")
  void cancelOrder_success() {
    Long memberId = 1L;
    Long orderId = 2L;
    Long storeId = 3L;
    Long sellerMemberId = 4L;
    Order order = mock(Order.class);
    StoreResult store = mock(StoreResult.class);

    when(orderRepository.findWithLockByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(storeService.getStore(storeId)).thenReturn(store);
    when(store.memberId()).thenReturn(sellerMemberId);

    Order result = orderService.cancelOrder(memberId, orderId);

    assertThat(result).isSameAs(order);
    verify(orderRepository, times(1)).findWithLockByIdAndIsDeletedFalse(orderId);
    verify(order, times(1)).cancel(memberId);
    verify(orderStatusChangedEventWriter).append(order);
    verify(orderNotificationEventWriter).appendCancelled(order, sellerMemberId);
  }

  @Test
  @DisplayName("주문을 접수하고 픽업 코드를 발급한다")
  void acceptOrder_success() {
    Long orderId = 1L;
    Long storeId = 2L;
    String pickupCode = "123456";
    Order order = mock(Order.class);

    when(orderRepository.findWithLockByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(pickupCodeGenerator.generate()).thenReturn(pickupCode);
    when(orderRepository.validateActivePickUpCode(storeId, pickupCode)).thenReturn(false);
    when(order.getId()).thenReturn(orderId);
    when(order.getPickupCode()).thenReturn(pickupCode);

    OrderReceptionResult response = orderService.acceptOrder(orderId);

    assertThat(response.orderId()).isEqualTo(orderId);
    assertThat(response.pickUpCode()).isEqualTo(pickupCode);
    verify(orderRepository, times(1)).findWithLockByIdAndIsDeletedFalse(orderId);
    verify(pickupCodeGenerator, times(1)).generate();
    verify(orderRepository, times(1)).validateActivePickUpCode(storeId, pickupCode);
    verify(order, times(1)).issuePickupCode(pickupCode);
    verify(orderStatusChangedEventWriter).append(order);
    verify(orderNotificationEventWriter).appendAccepted(order);
  }

  @Test
  @DisplayName("픽업 코드가 중복되면 사용 가능한 코드를 다시 발급한다")
  void acceptOrder_regeneratesPickupCodeWhenDuplicated() {
    Long orderId = 1L;
    Long storeId = 2L;
    String duplicatedCode = "123456";
    String availableCode = "654321";
    Order order = mock(Order.class);

    when(orderRepository.findWithLockByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(pickupCodeGenerator.generate()).thenReturn(duplicatedCode, availableCode);
    when(orderRepository.validateActivePickUpCode(storeId, duplicatedCode)).thenReturn(true);
    when(orderRepository.validateActivePickUpCode(storeId, availableCode)).thenReturn(false);
    when(order.getId()).thenReturn(orderId);
    when(order.getPickupCode()).thenReturn(availableCode);

    OrderReceptionResult response = orderService.acceptOrder(orderId);

    assertThat(response.orderId()).isEqualTo(orderId);
    assertThat(response.pickUpCode()).isEqualTo(availableCode);
    verify(pickupCodeGenerator, times(2)).generate();
    verify(orderRepository, times(1)).validateActivePickUpCode(storeId, duplicatedCode);
    verify(orderRepository, times(1)).validateActivePickUpCode(storeId, availableCode);
    verify(order, never()).issuePickupCode(duplicatedCode);
    verify(order, times(1)).issuePickupCode(availableCode);
  }

  @Test
  @DisplayName("픽업 코드 생성 재시도 횟수를 초과하면 예외가 발생한다")
  void acceptOrder_throwsExceptionWhenPickupCodeGenerationExceedsMaxRetry() {
    Long orderId = 1L;
    Long storeId = 2L;
    String duplicatedCode = "123456";
    Order order = mock(Order.class);

    when(orderRepository.findWithLockByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(pickupCodeGenerator.generate()).thenReturn(duplicatedCode);
    when(orderRepository.validateActivePickUpCode(storeId, duplicatedCode)).thenReturn(true);

    assertThatThrownBy(() -> orderService.acceptOrder(orderId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PICKUP_CODE_GENERATION_FAILED);

    verify(orderRepository, times(1)).findWithLockByIdAndIsDeletedFalse(orderId);
    verify(pickupCodeGenerator, times(5)).generate();
    verify(orderRepository, times(5)).validateActivePickUpCode(storeId, duplicatedCode);
    verify(order, never()).issuePickupCode(anyString());
  }

  @Test
  @DisplayName("주문을 픽업 완료 상태로 변경한다")
  void updatePickupStatus_pickedUp_success() {
    Long orderId = 1L;
    Order order = mock(Order.class);
    when(orderRepository.findWithLockByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.nextEventVersion()).thenReturn(7L);

    PickupStatusResult response =
        orderService.updatePickupStatus(
            orderId, new UpdatePickupStatusCommand(OrderStatus.PICKED_UP));

    assertThat(response).isNotNull();
    verify(orderRepository).findWithLockByIdAndIsDeletedFalse(orderId);
    verify(order).completePickup(any(LocalDateTime.class));
    verify(order).nextEventVersion();
    verify(orderStatusChangedEventWriter).append(order, 7L);
    verify(orderNotificationEventWriter).appendPickedUp(order);
  }

  @Test
  @DisplayName("주문을 노쇼 상태로 변경한다")
  void updatePickupStatus_noShow_success() {
    Long orderId = 1L;
    Order order = mock(Order.class);
    when(orderRepository.findWithLockByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.nextEventVersion()).thenReturn(7L);

    PickupStatusResult response =
        orderService.updatePickupStatus(
            orderId, new UpdatePickupStatusCommand(OrderStatus.NO_SHOW));

    assertThat(response).isNotNull();
    verify(orderRepository).findWithLockByIdAndIsDeletedFalse(orderId);
    verify(order).markNoShow(any(LocalDateTime.class));
    verify(order, never()).completePickup(any(LocalDateTime.class));
    verify(order).nextEventVersion();
    verify(orderStatusChangedEventWriter).append(order, 7L);
    verify(orderNotificationEventWriter).appendNoShow(order);
  }

  @Test
  @DisplayName("허용되지 않는 픽업 상태 변경 요청은 거부한다")
  void updatePickupStatus_invalidStatus() {
    assertThatThrownBy(() -> new UpdatePickupStatusCommand(OrderStatus.RESERVED))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("픽업 상태는 PICKED_UP 또는 NO_SHOW만 가능합니다.");

    verifyNoInteractions(
        orderRepository,
        orderStatusChangedEventWriter,
        orderPickedUpEventWriter,
        orderNoShowEventWriter);
  }

  @Test
  @DisplayName("회원의 주문 한 건을 조회한다")
  void getEachOrder_success() {
    Long orderId = 1L;
    Long memberId = 1L;
    Order order = mock(Order.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    doNothing().when(order).validateOwner(memberId);
    when(order.getRejectReason()).thenReturn(null);

    OrderResult response = orderService.getEachOrder(memberId, orderId);

    assertThat(response).isNotNull();
    verify(orderRepository).findByIdAndIsDeletedFalse(orderId);
    verify(order).validateOwner(memberId);
  }

  @Test
  @DisplayName("상태 조건으로 회원 주문 목록을 조회한다")
  void getMyOrders_success() {
    Long memberId = 1L;
    OrderStatus status = OrderStatus.RESERVED;
    Pageable pageable = PageRequest.of(0, 20);
    Order order = mock(Order.class);
    Page<Order> orders = new PageImpl<>(List.of(order), pageable, 1);

    when(orderRepository.findAllByMemberIdAndStatus(memberId, status, pageable)).thenReturn(orders);

    Page<OrderResult> response = orderService.getMyOrders(memberId, status, pageable);

    assertThat(response.getTotalElements()).isEqualTo(1);
    assertThat(response.getContent()).hasSize(1);
    verify(orderRepository, times(1)).findAllByMemberIdAndStatus(memberId, status, pageable);
  }

  @Test
  @DisplayName("상태 조건 없이 회원 주문 목록을 조회한다")
  void getMyOrders_withoutStatus_success() {
    Long memberId = 1L;
    Pageable pageable = PageRequest.of(0, 20);

    when(orderRepository.findAllByMemberIdAndStatus(memberId, null, pageable))
        .thenReturn(Page.empty(pageable));

    Page<OrderResult> response = orderService.getMyOrders(memberId, null, pageable);

    assertThat(response).isEmpty();
    verify(orderRepository, times(1)).findAllByMemberIdAndStatus(memberId, null, pageable);
  }

  @Test
  @DisplayName("상태 조건으로 매장 주문 목록을 조회한다")
  void getStoreOrders_success() {
    Long storeId = 1L;
    OrderStatus status = OrderStatus.PICKUP_READY;
    Pageable pageable = PageRequest.of(0, 20);
    Order order = mock(Order.class);
    Page<Order> orders = new PageImpl<>(List.of(order), pageable, 1);

    when(orderRepository.findAllByStoreIdAndStatus(storeId, status, pageable)).thenReturn(orders);

    Page<OrderResult> response = orderService.getStoreOrders(storeId, status, pageable);

    assertThat(response.getTotalElements()).isEqualTo(1);
    assertThat(response.getContent()).hasSize(1);
    verify(orderRepository, times(1)).findAllByStoreIdAndStatus(storeId, status, pageable);
  }

  @Test
  @DisplayName("상태 조건 없이 매장 주문 목록을 조회한다")
  void getStoreOrders_withoutStatus_success() {
    Long storeId = 1L;
    Pageable pageable = PageRequest.of(0, 20);

    when(orderRepository.findAllByStoreIdAndStatus(storeId, null, pageable))
        .thenReturn(Page.empty(pageable));

    Page<OrderResult> response = orderService.getStoreOrders(storeId, null, pageable);

    assertThat(response).isEmpty();
    verify(orderRepository, times(1)).findAllByStoreIdAndStatus(storeId, null, pageable);
  }
}
