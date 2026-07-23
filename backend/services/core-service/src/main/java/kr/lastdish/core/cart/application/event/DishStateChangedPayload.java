package kr.lastdish.core.cart.application.event;

/**
 * Dish 상태 변경 이벤트에서 Cart가 사용하는 업무 데이터입니다.
 *
 * <p>Dish 식별자와 이벤트 버전 같은 공통 메타데이터는 EventMessage에서 가져옵니다.
 */
public record DishStateChangedPayload(boolean available, Long stockQuantity) {}
