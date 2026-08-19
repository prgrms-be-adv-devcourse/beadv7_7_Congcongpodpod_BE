package kr.lastdish.core.order.application;

import java.time.LocalDateTime;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.cart.application.CartFacade;
import kr.lastdish.core.cart.application.dto.CartOrderSnapshot;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.dish.application.DishFacade;
import kr.lastdish.core.order.application.dto.*;
import kr.lastdish.core.order.application.event.OrderStatusChangedEventWriter;
import kr.lastdish.core.order.application.port.out.OrderMemberQueryPort;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.domain.OrderRejectReason;
import kr.lastdish.core.order.domain.OrderRepository;
import kr.lastdish.core.order.domain.OrderStatus;
import kr.lastdish.core.payment.application.deposit.DepositFacade;
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
  private final OrderStatusChangedEventWriter orderStatusChangedEventWriter;
  private final OrderService orderService;
  private final CartFacade cartFacade;
  private final DishFacade dishFacade;
  private final DepositFacade depositFacade;
  private final StoreFacade storeFacade;
  private final OrderMemberQueryPort orderMemberQueryPort;

  // 주문 생성 - 재고 차감 - 결제
  @Transactional
  public OrderResult payAndCreateOrder(Long memberId, Long cartItemId) {
    // 외부 회원 서비스 호출을 먼저 완료해 CartItem DB 잠금 시간을 최소화한다.
    OrderMemberInfo memberInfo = orderMemberQueryPort.getOrderMemberInfo(memberId);
    CartOrderSnapshot cartItem = cartFacade.getOrderSnapshot(memberId, cartItemId);

    // 사용자가 장바구니에서 확인한 판매가와 현재 판매가가 다르면 결제를 진행하지 않는다.
    dishFacade.validateOrderPrice(cartItem.dishId(), cartItem.unitPrice());
    storeFacade.validateOpen(cartItem.storeId());
    orderService.validatePickupDeadline(cartItem);

    // 주문 생성 및 저장
    Order order = orderService.createOrder(memberId, memberInfo, cartItem);

    // 재고 차감
    dishFacade.decreaseStock(order.getDishId(), order.getQuantity());

    // 예치금 사용
    depositFacade.use(memberId, order.getId(), order.getTotalPrice());

    // 결제 완료 처리
    OrderResult result = orderService.completePayment(order.getId());

    // 주문이 완료된 상품을 장바구니에서 제거
    cartFacade.removeOrderedItem(memberId, cartItemId);

    return result;
  }

  // 주문 취소 - 재고 복구 - 결제 환불
  @Transactional
  public OrderResult cancelOrder(Long memberId, Long orderId) {
    // 주문 취소
    Order order = orderService.cancelOrder(memberId, orderId);

    // 재고 복구
    dishFacade.increaseStock(order.getDishId(), order.getQuantity());

    // 결제 환불
    depositFacade.refund(memberId, orderId, order.getTotalPrice());

    return OrderResult.from(order);
  }

  // 매장 주문 접수
  @Transactional
  public OrderReceptionResult acceptOrder(Long memberId, String role, Long orderId) {

    validateSellerOrder(memberId, role, orderId);

    // 주문 접수, 픽업 코드 발급
    return orderService.acceptOrder(orderId);
  }

  // 매장 주문 반려
  @Transactional
  public OrderRejectResult rejectOrder(
      Long memberId, String role, Long orderId, RejectOrderCommand command) {
    validateSellerOrder(memberId, role, orderId);
    OrderRejectReason reason = command.reason();

    // 반려 사유에 따라 환불 프로세스 분기
    if (reason.shouldRestoreStock()) {
      return rejectOrderAndRestoreStock(orderId, reason);
    } else {
      return rejectOrder(orderId, reason);
    }
  }

  @Transactional
  public OrderRejectResult rejectOrderAndRestoreStock(Long orderId, OrderRejectReason reason) {
    Order order = orderRepository.findWithLockByIdAndIsDeletedFalse(orderId);
    // 재고 복구
    dishFacade.increaseStock(order.getDishId(), order.getQuantity());
    order.rejectOrder(reason);
    orderStatusChangedEventWriter.append(order);
    // 환불
    depositFacade.refund(order.getMemberId(), orderId, order.getTotalPrice());
    return OrderRejectResult.from(order);
  }

  @Transactional
  public OrderRejectResult rejectOrder(Long orderId, OrderRejectReason reason) {
    Order order = orderRepository.findWithLockByIdAndIsDeletedFalse(orderId);
    order.rejectOrder(reason);
    orderStatusChangedEventWriter.append(order);
    // 환불 - 재고 복구 안함
    depositFacade.refund(order.getMemberId(), orderId, order.getTotalPrice());
    return OrderRejectResult.from(order);
  }

  @Transactional
  public PickupStatusResult updateOrder(
      Long memberId, String role, Long orderId, UpdatePickupStatusCommand command) {
    validateSellerOrder(memberId, role, orderId);

    // 상태 업데이트
    return orderService.updatePickupStatus(orderId, command);
  }

  @Transactional(readOnly = true)
  public Page<OrderResult> getStoreOrders(
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
