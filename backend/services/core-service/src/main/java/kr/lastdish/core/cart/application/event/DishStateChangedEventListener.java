package kr.lastdish.core.cart.application.event;

import kr.lastdish.core.cart.application.CartDishStateSynchronizer;
import kr.lastdish.core.dish.domain.event.DishStateChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spring Event로 전달된 Dish 상태 변경을 Cart 상태 동기화 서비스에 연결합니다.
 *
 * <p>이 Listener는 Spring Event 전달만 담당하고 CartItem 변경 규칙과 트랜잭션은 CartDishStateSynchronizer가 관리합니다.
 */
@Component
@RequiredArgsConstructor
public class DishStateChangedEventListener {

  private final CartDishStateSynchronizer synchronizer;

  @EventListener
  public void handle(DishStateChangedEvent event) {
    synchronizer.synchronize(
        event.dishId(), event.available(), event.stockQuantity(), event.aggregateVersion());
  }
}
