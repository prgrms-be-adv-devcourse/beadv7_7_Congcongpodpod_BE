package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.dish.application.DishFacade;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRejectReason;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.order.presentation.dto.*;
import kr.lastdish.core.payment.application.DepositFacade;
import kr.lastdish.core.store.application.StoreFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFacade {

  private final OrderRepository orderRepository;
  private final OrderService orderService;
  private final DishFacade dishFacade;
  private final DepositFacade depositFacade;
  private final StoreFacade storeFacade;

  // 주문 생성 - 재고 차감 - 결제
  @Transactional
  public OrderResponse payAndCreateOrder(Long memberId, OrderCreateRequest request) {
    // 주문 생성 및 저장
    Order order = orderService.createOrder(memberId, request);

    // 재고 차감
    dishFacade.decreaseStock(order.getDishId(), order.getQuantity());

    // 예치금 사용
    depositFacade.use(memberId, order.getId(), order.getTotalPrice());

    // 결제 완료 처리
    return orderService.completePayment(order.getId());
  }

  // 주문 취소 - 재고 복구 - 결제 환불
  @Transactional
  public OrderResponse cancelOrder(Long memberId, Long orderId) {
    // 주문 취소
    Order order = orderService.cancelOrder(memberId, orderId);

    // 재고 복구
    dishFacade.increaseStock(order.getDishId(), order.getQuantity());

    // 결제 환불
    depositFacade.refund(memberId, orderId, order.getTotalPrice());

    return OrderResponse.from(order);
  }

  // 매장 주문 접수
  @Transactional
  public OrderReceptionResponse acceptOrder(Long memberId, String role, Long orderId) {

    validateSellerOrder(memberId, role, orderId);

    // 주문 접수, 픽업 코드 발급
    return orderService.acceptOrder(orderId);
  }

  // 매장 주문 반려
  @Transactional
  public OrderRejectResponse rejectOrder(
      Long memberId, String role, Long orderId, OrderRejectRequest request) {
    validateSellerOrder(memberId, role, orderId);

    // 반려 사유에 따라 환불 프로세스 분기
    if (request.reason().shouldRestoreStock()) {
      return rejectOrderAndRestoreStock(orderId, request.reason());
    } else {
      return rejectOrder(orderId, request.reason());
    }
  }

  @Transactional
  public OrderRejectResponse rejectOrderAndRestoreStock(Long orderId, OrderRejectReason reason) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);
    order.rejectOrder(reason);
    // 환불
    depositFacade.refund(order.getMemberId(), orderId, order.getTotalPrice());
    // 재고 복구
    dishFacade.increaseStock(order.getDishId(), order.getQuantity());
    return OrderRejectResponse.from(order);
  }

  @Transactional
  public OrderRejectResponse rejectOrder(Long orderId, OrderRejectReason reason) {
    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);
    order.rejectOrder(reason);
    // 환불 - 재고 복구 안함
    depositFacade.refund(order.getMemberId(), orderId, order.getTotalPrice());
    return OrderRejectResponse.from(order);
  }

  @Transactional
  public PickupStatusResponse updateOrder(
      Long memberId, String role, Long orderId, PickupStatusRequest request) {
    validateSellerOrder(memberId, role, orderId);

    // 상태 업데이트
    return orderService.updatePickupStatus(orderId, request);
  }

  @Transactional(readOnly = true)
  public Page<OrderResponse> getStoreOrders(
      Long memberId, String role, Long storeId, OrderStatus status, Pageable pageable) {
    validateSeller(role);
    storeFacade.validateStoreOwner(storeId, memberId);
    return orderService.getStoreOrders(storeId, status, pageable);
  }

  private void validateSeller(String role) {
    if (!"SELLER".equals(role)) {
      throw new BusinessException(ErrorCode.ORDER_NOT_SELLER);
    }
  }

  private void validateSellerOrder(Long memberId, String role, Long orderId) {
    validateSeller(role);

    Order order = orderRepository.findByIdAndIsDeletedFalse(orderId);
    storeFacade.validateStoreOwner(order.getStoreId(), memberId);
  }

  public List<OrderSettlementInfo> findSettlementOrders(
      Long storeId, LocalDateTime periodStart, LocalDateTime periodEnd) {
    return orderService.findSettlementOrders(storeId, periodStart, periodEnd);
  }
}
