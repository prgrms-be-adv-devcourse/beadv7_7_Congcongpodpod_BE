package kr.lastdish.core.order.application;

import kr.lastdish.core.dish.application.DishFacade;
import kr.lastdish.core.order.domain.Order;
import kr.lastdish.core.order.presentation.dto.OrderCancelRequest;
import kr.lastdish.core.order.presentation.dto.OrderCreateRequest;
import kr.lastdish.core.order.presentation.dto.OrderResponse;
import kr.lastdish.core.payment.application.DepositService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderFacade {

  private final OrderService orderService;
  private final DishFacade dishFacade;
  private final DepositService depositService;

  // 주문 생성 - 재고 차감 - 결제
  @Transactional
  public OrderResponse payAndCreateOrder(Long memberId, OrderCreateRequest request) {
    // 주문 생성 및 저장
    Order order = orderService.createOrder(memberId, request);

    // 재고 차감
    dishFacade.decreaseStock(order.getDishId(), order.getQuantity());

    // 예치금 사용
    depositService.use(memberId, order.getId(), order.getTotalPrice());

    // 결제 완료 처리
    return orderService.completePayment(order.getId());
  }

  // 주문 취소 - 결제 환불 - 재고 복구
  @Transactional
  public OrderResponse cancelOrder(Long memberId, Long orderId, OrderCancelRequest request) {
    // 주문 취소
    Order order = orderService.cancelOrder(memberId, orderId, request);

    // 결제 환불
    depositService.refund(memberId, orderId, order.getTotalPrice());

    // 재고 복구
    dishFacade.increaseStock(order.getDishId(), order.getQuantity());

    return OrderResponse.from(order);
  }
}
