package kr.lastdish.core.dish.domain.event;

/**
 * Dish 상태 변경 이벤트가 Consumer에게 전달하는 업무 데이터입니다.
 *
 * <p>이벤트 식별자와 Aggregate 정보 등의 공통 메타데이터는 DomainEvent에서 관리합니다.
 */
public record DishStateChangedPayload(boolean available, Long stockQuantity) {}
