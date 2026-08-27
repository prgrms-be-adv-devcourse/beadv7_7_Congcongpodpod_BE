package kr.lastdish.core.order.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.cart.application.CartFacade;
import kr.lastdish.core.cart.application.dto.CartOrderSnapshot;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.deposit.application.DepositFacade;
import kr.lastdish.core.dish.application.DishFacade;
import kr.lastdish.core.order.application.dto.*;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFacade {
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

  private final OrderRepository orderRepository;
  private final OrderStatusChangedEventWriter orderStatusChangedEventWriter;
  private final OrderNotificationEventWriter orderNotificationEventWriter;
  private final OrderService orderService;
  private final CartFacade cartFacade;
  private final DishFacade dishFacade;
  private final DepositFacade depositFacade;
  private final StoreFacade storeFacade;
  private final PointService pointService;
  private final MemberSnapshotRepository memberSnapshotRepository;

  // 주문 생성 - 재고 차감 - 결제
  @Transactional
  public OrderResult payAndCreateOrder(
      Long memberId, Long cartItemId, Long dishPriceVersion, BigDecimal usedPoint) {
    MemberSnapshot memberSnapshot =
        memberSnapshotRepository
            .findActiveByMemberId(memberId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_MEMBER_SNAPSHOT_NOT_FOUND));
    OrderMemberInfo memberInfo =
        new OrderMemberInfo(memberSnapshot.getName(), memberSnapshot.getPhone());

    // 가격 변경 검증
    CartOrderSnapshot cartItem =
        cartFacade.getValidatedOrderSnapshot(memberId, cartItemId, dishPriceVersion);

    // 서버 현재 시각을 한 번만 구해 자정 경계를 포함한 픽업 마감 일시를 계산한다.
    LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
    LocalDateTime pickupDeadline = validateBeforeOrder(cartItem, now);

    // 주문 생성 및 저장
    Order order =
        orderService.createOrder(memberId, memberInfo, cartItem, usedPoint, pickupDeadline);

    // 재고 차감
    dishFacade.decreaseStock(order.getDishId(), order.getQuantity());

    // 포인트 사용
    pointService.use(memberId, order.getId(), usedPoint);

    // 예치금 사용
    depositFacade.use(memberId, order.getId(), order.getUsedDeposit());

    // 결제 완료 처리
    OrderResult result = orderService.completePayment(order.getId());

    // 주문이 완료된 상품을 장바구니에서 제거
    cartFacade.removeOrderedItem(memberId, cartItemId);

    Long sellerMemberId = storeFacade.getStoreOwnerMemberId(order.getStoreId());
    orderNotificationEventWriter.appendCreated(order, sellerMemberId);

    return result;
  }

  private LocalDateTime validateBeforeOrder(CartOrderSnapshot cartItem, LocalDateTime now) {
    storeFacade.validateOpen(cartItem.storeId());
    return orderService.validatePickupDeadline(cartItem, now);
  }

  // 주문 취소 - 재고 복구 - 결제 환불
  @Transactional
  public OrderResult cancelOrder(Long memberId, Long orderId) {
    // 주문 취소
    Order order = orderService.cancelOrder(memberId, orderId);

    // 재고 복구
    dishFacade.increaseStock(order.getDishId(), order.getQuantity());

    // 포인트 환불

    // 결제 환불
    depositFacade.refund(memberId, orderId, order.getTotalPrice());

    Long sellerMemberId = storeFacade.getStoreOwnerMemberId(order.getStoreId());
    orderNotificationEventWriter.appendCancelled(order, sellerMemberId);

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
    orderNotificationEventWriter.appendRejected(order, reason);
    // 환불
    // 포인트 환불
    depositFacade.refund(order.getMemberId(), orderId, order.getTotalPrice());
    return OrderRejectResult.from(order);
  }

  @Transactional
  public OrderRejectResult rejectOrder(Long orderId, OrderRejectReason reason) {
    Order order = orderRepository.findWithLockByIdAndIsDeletedFalse(orderId);
    order.rejectOrder(reason);
    orderStatusChangedEventWriter.append(order);
    orderNotificationEventWriter.appendRejected(order, reason);
    // 환불 - 재고 복구 안함
    // 포인트 환불
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
