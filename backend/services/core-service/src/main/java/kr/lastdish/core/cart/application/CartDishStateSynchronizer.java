package kr.lastdish.core.cart.application;

import java.util.List;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dish의 최신 판매 가능 여부와 재고를 CartItem 상태에 반영합니다.
 *
 * <p>Spring Event와 향후 Kafka Consumer가 동일한 Cart 상태 변경 로직을 재사용하도록 전송 기술과 비즈니스 로직을 분리합니다.
 */
@Service
@RequiredArgsConstructor
public class CartDishStateSynchronizer {

  private final CartItemRepository cartItemRepository;

  /**
   * 같은 Dish를 담고 있는 모든 CartItem의 주문 가능 상태를 갱신합니다.
   *
   * <p>기존 트랜잭션이 있으면 참여하고, Kafka Consumer처럼 별도 호출되는 경우에는 새로운 트랜잭션을 시작합니다.
   *
   * @param dishId 변경된 Dish 식별자
   * @param available Dish 판매 가능 여부
   * @param stockQuantity 현재 Dish 재고
   */
  @Transactional
  public void synchronize(Long dishId, boolean available, Long stockQuantity) {

    List<CartItem> cartItems = cartItemRepository.findAllByDishId(dishId);

    for (CartItem cartItem : cartItems) {
      cartItem.synchronizeDishState(available, stockQuantity);
    }
  }
}
