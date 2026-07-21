package kr.lastdish.core.order.application;

import kr.lastdish.core.order.domain.Order;
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
  private final DepositService depositService;

  // 주문 생성 후 결제 로직 - 하나의 트랜잭션 안에서 수행
  @Transactional
  public OrderResponse payAndCreateOrder(OrderCreateRequest request) {
      // 주문 생성
      Order order = orderService.createOrder(request);

      // 예치금 사용
      depositService.use(
        request.memberId(),
        order.getId(),
        request.totalPrice());

      // 결제 완료 처리
      return orderService.completePayment(order.getId());
  }
}
