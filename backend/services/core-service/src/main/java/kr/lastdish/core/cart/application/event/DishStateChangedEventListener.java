package kr.lastdish.core.cart.application.event;

import java.util.List;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import kr.lastdish.core.common.event.dish.DishStateChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dish 상태 변경을 CartItem의 주문 가능 상태에 반영합니다.
 *
 * <p>Spring Event는 현재 OutboxEventProcessor의 트랜잭션 안에서 동기로 전달됩니다. Listener 처리 중 예외가 발생하면 CartItem 변경과
 * Outbox의 PUBLISHED 변경이 함께 롤백됩니다.
 */
@Component
@RequiredArgsConstructor
public class DishStateChangedEventListener {

  private final CartItemRepository cartItemRepository;

  /**
   * 같은 Dish를 담고 있는 모든 CartItem의 상태를 갱신합니다.
   *
   * <p>기본 전파 속성인 REQUIRED를 사용하므로 OutboxEventProcessor의 트랜잭션에 참여합니다.
   */
  @EventListener
  @Transactional
  public void handle(DishStateChangedEvent event) {
    List<CartItem> cartItems = cartItemRepository.findAllByDishId(event.dishId());

    for (CartItem cartItem : cartItems) {
      cartItem.synchronizeDishState(event.available(), event.stockQuantity());
    }
  }
}
