package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import kr.lastdish.core.order.presentation.dto.OrderReceptionResponse;
import kr.lastdish.core.order.presentation.dto.OrderResponse;
import kr.lastdish.core.order.presentation.dto.PickupStatusRequest;
import kr.lastdish.core.order.presentation.dto.PickupStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

  @Mock private OrderRepository orderRepository;

  @Mock private PickupCodeGenerator pickupCodeGenerator;

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

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);

    Order result = orderService.cancelOrder(memberId, orderId);

    assertThat(result).isSameAs(order);
    verify(orderRepository, times(1)).findByIdAndIsDeletedFalse(orderId);
    verify(order, times(1)).cancel(memberId);
  }

  @Test
  void acceptOrder_success() {
    Long orderId = 1L;
    Long storeId = 2L;
    String pickupCode = "123456";
    Order order = mock(Order.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(pickupCodeGenerator.generate()).thenReturn(pickupCode);
    when(orderRepository.validateActivePickUpCode(storeId, pickupCode)).thenReturn(false);
    when(order.getId()).thenReturn(orderId);
    when(order.getPickupCode()).thenReturn(pickupCode);

    OrderReceptionResponse response = orderService.acceptOrder(orderId);

    assertThat(response.orderId()).isEqualTo(orderId);
    assertThat(response.pickUpCode()).isEqualTo(pickupCode);
    verify(orderRepository, times(1)).findByIdAndIsDeletedFalse(orderId);
    verify(pickupCodeGenerator, times(1)).generate();
    verify(orderRepository, times(1)).validateActivePickUpCode(storeId, pickupCode);
    verify(order, times(1)).issuePickupCode(pickupCode);
  }

  @Test
  void acceptOrder_regeneratesPickupCodeWhenDuplicated() {
    Long orderId = 1L;
    Long storeId = 2L;
    String duplicatedCode = "123456";
    String availableCode = "654321";
    Order order = mock(Order.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(pickupCodeGenerator.generate()).thenReturn(duplicatedCode, availableCode);
    when(orderRepository.validateActivePickUpCode(storeId, duplicatedCode)).thenReturn(true);
    when(orderRepository.validateActivePickUpCode(storeId, availableCode)).thenReturn(false);
    when(order.getId()).thenReturn(orderId);
    when(order.getPickupCode()).thenReturn(availableCode);

    OrderReceptionResponse response = orderService.acceptOrder(orderId);

    assertThat(response.orderId()).isEqualTo(orderId);
    assertThat(response.pickUpCode()).isEqualTo(availableCode);
    verify(pickupCodeGenerator, times(2)).generate();
    verify(orderRepository, times(1)).validateActivePickUpCode(storeId, duplicatedCode);
    verify(orderRepository, times(1)).validateActivePickUpCode(storeId, availableCode);
    verify(order, never()).issuePickupCode(duplicatedCode);
    verify(order, times(1)).issuePickupCode(availableCode);
  }

  @Test
  void acceptOrder_throwsExceptionWhenPickupCodeGenerationExceedsMaxRetry() {
    Long orderId = 1L;
    Long storeId = 2L;
    String duplicatedCode = "123456";
    Order order = mock(Order.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(order.getStoreId()).thenReturn(storeId);
    when(pickupCodeGenerator.generate()).thenReturn(duplicatedCode);
    when(orderRepository.validateActivePickUpCode(storeId, duplicatedCode)).thenReturn(true);

    assertThatThrownBy(() -> orderService.acceptOrder(orderId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.PICKUP_CODE_GENERATION_FAILED);

    verify(orderRepository, times(1)).findByIdAndIsDeletedFalse(orderId);
    verify(pickupCodeGenerator, times(5)).generate();
    verify(orderRepository, times(5)).validateActivePickUpCode(storeId, duplicatedCode);
    verify(order, never()).issuePickupCode(anyString());
  }

  @Test
  void updatePickupStatus_pickedUp_success() {
    Long orderId = 1L;
    Order order = mock(Order.class);
    PickupStatusRequest request = mock(PickupStatusRequest.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(request.status()).thenReturn(OrderStatus.PICKED_UP);

    PickupStatusResponse response = orderService.updatePickupStatus(orderId, request);

    assertThat(response).isNotNull();
    verify(orderRepository).findByIdAndIsDeletedFalse(orderId);
    verify(order).completePickup();
    verify(order, never()).markNoShow();
  }

  @Test
  void updatePickupStatus_noShow_success() {
    Long orderId = 1L;
    Order order = mock(Order.class);
    PickupStatusRequest request = mock(PickupStatusRequest.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(request.status()).thenReturn(OrderStatus.NO_SHOW);

    PickupStatusResponse response = orderService.updatePickupStatus(orderId, request);

    assertThat(response).isNotNull();
    verify(orderRepository).findByIdAndIsDeletedFalse(orderId);
    verify(order).markNoShow();
    verify(order, never()).completePickup();
  }

  @Test
  void updatePickupStatus_invalidStatus() {
    Long orderId = 1L;
    Order order = mock(Order.class);
    PickupStatusRequest request = mock(PickupStatusRequest.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    when(request.status()).thenReturn(OrderStatus.RESERVED);

    assertThatThrownBy(() -> orderService.updatePickupStatus(orderId, request))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(CommonErrorCode.INVALID_STATE);

    verify(orderRepository).findByIdAndIsDeletedFalse(orderId);
    verify(order, never()).completePickup();
    verify(order, never()).markNoShow();
  }

  @Test
  void getEachOrder_success() {
    Long orderId = 1L;
    Long memberId = 1L;
    Order order = mock(Order.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);
    doNothing().when(order).validateOwner(memberId);
    when(order.getRejectReason()).thenReturn(null);

    OrderResponse response = orderService.getEachOrder(memberId, orderId);

    assertThat(response).isNotNull();
    verify(orderRepository).findByIdAndIsDeletedFalse(orderId);
    verify(order).validateOwner(memberId);
  }

  @Test
  void getMyOrders_success() {
    Long memberId = 1L;
    OrderStatus status = OrderStatus.RESERVED;
    Pageable pageable = PageRequest.of(0, 20);
    Order order = mock(Order.class);
    Page<Order> orders = new PageImpl<>(List.of(order), pageable, 1);

    when(orderRepository.findAllByMemberIdAndStatus(memberId, status, pageable)).thenReturn(orders);

    Page<OrderResponse> response = orderService.getMyOrders(memberId, status, pageable);

    assertThat(response.getTotalElements()).isEqualTo(1);
    assertThat(response.getContent()).hasSize(1);
    verify(orderRepository, times(1)).findAllByMemberIdAndStatus(memberId, status, pageable);
  }

  @Test
  void getMyOrders_withoutStatus_success() {
    Long memberId = 1L;
    Pageable pageable = PageRequest.of(0, 20);

    when(orderRepository.findAllByMemberIdAndStatus(memberId, null, pageable))
        .thenReturn(Page.empty(pageable));

    Page<OrderResponse> response = orderService.getMyOrders(memberId, null, pageable);

    assertThat(response).isEmpty();
    verify(orderRepository, times(1)).findAllByMemberIdAndStatus(memberId, null, pageable);
  }

  @Test
  void getStoreOrders_success() {
    Long storeId = 1L;
    OrderStatus status = OrderStatus.PICKUP_READY;
    Pageable pageable = PageRequest.of(0, 20);
    Order order = mock(Order.class);
    Page<Order> orders = new PageImpl<>(List.of(order), pageable, 1);

    when(orderRepository.findAllByStoreIdAndStatus(storeId, status, pageable)).thenReturn(orders);

    Page<OrderResponse> response = orderService.getStoreOrders(storeId, status, pageable);

    assertThat(response.getTotalElements()).isEqualTo(1);
    assertThat(response.getContent()).hasSize(1);
    verify(orderRepository, times(1)).findAllByStoreIdAndStatus(storeId, status, pageable);
  }

  @Test
  void getStoreOrders_withoutStatus_success() {
    Long storeId = 1L;
    Pageable pageable = PageRequest.of(0, 20);

    when(orderRepository.findAllByStoreIdAndStatus(storeId, null, pageable))
        .thenReturn(Page.empty(pageable));

    Page<OrderResponse> response = orderService.getStoreOrders(storeId, null, pageable);

    assertThat(response).isEmpty();
    verify(orderRepository, times(1)).findAllByStoreIdAndStatus(storeId, null, pageable);
  }
}
