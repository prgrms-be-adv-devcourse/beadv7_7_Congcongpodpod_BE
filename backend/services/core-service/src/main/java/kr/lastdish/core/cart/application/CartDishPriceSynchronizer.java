package kr.lastdish.core.cart.application;

import java.math.BigDecimal;
import java.util.List;
import kr.lastdish.core.cart.domain.CartItem;
import kr.lastdish.core.cart.domain.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dish 최신 판매 가격을 해당 Dish를 담은 CartItem에 반영합니다.
 *
 * <p>Spring Event와 향후 Kafka Consumer가 같은 가격 동기화 로직을 재사용할 수 있도록 메시지 처리와 비즈니스 로직을 분리합니다.
 */
@Service
@RequiredArgsConstructor
public class CartDishPriceSynchronizer {

  private final CartItemRepository cartItemRepository;

  /**
   * 같은 Dish를 담고 있는 모든 CartItem의 판매 단가를 갱신합니다.
   *
   * @param dishId 변경된 Dish 식별자
   * @param unitPrice Dish의 현재 판매 가격
   * @param aggregateVersion Dish 가격 변경 이벤트 순서
   */
  @Transactional
  public void synchronize(Long dishId, BigDecimal unitPrice, long aggregateVersion) {

    List<CartItem> cartItems = cartItemRepository.findAllByDishId(dishId);

    for (CartItem cartItem : cartItems) {
      cartItem.synchronizeDishPrice(unitPrice, aggregateVersion);
    }
  }
}
