package kr.lastdish.core.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalTime;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import kr.lastdish.core.order.presentation.dto.OrderReceptionResponse;
import kr.lastdish.core.order.presentation.dto.PickupStatusRequest;
import kr.lastdish.core.order.presentation.dto.PickupStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
  void updatePickupStatus_success() {
    Long orderId = 1L;
    Order order = mock(Order.class);
    PickupStatusRequest request = mock(PickupStatusRequest.class);

    when(orderRepository.findByIdAndIsDeletedFalse(orderId)).thenReturn(order);

    PickupStatusResponse response = orderService.updatePickupStatus(orderId, request);

    assertThat(response).isNotNull();
    verify(orderRepository, times(1)).findByIdAndIsDeletedFalse(orderId);
    verify(order, times(1)).updateOrderStatus(request.status());
  }
}
