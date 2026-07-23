package kr.lastdish.core.cart.domain;

/**
 * 장바구니 상품의 현재 주문 가능 상태입니다.
 *
 * <p>이 값은 Cart가 소유하는 파생 데이터입니다. 실제 Dish 상태가 변경되면 이벤트를 통해 최종적으로 갱신됩니다.
 */
public enum CartItemStatus {

  /** Dish가 판매 중이고 장바구니 수량만큼 재고가 존재합니다. */
  AVAILABLE,

  /** Dish 재고는 존재하지만 장바구니에 담긴 수량보다 부족합니다. */
  INSUFFICIENT_STOCK,

  /** Dish 재고가 모두 소진되었습니다. */
  OUT_OF_STOCK,

  /** 판매 중지, 삭제 등으로 Dish를 주문할 수 없습니다. */
  DISH_UNAVAILABLE
}
