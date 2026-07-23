package kr.lastdish.core.dish.domain.event;

import java.time.Instant;
import java.util.UUID;
import kr.lastdish.common.event.DomainEvent;

/**
 * Cart 등 다른 도메인이 알아야 하는 Dish 주문 상태가 변경되었음을 나타냅니다.
 *
 * <p>판매 가능 여부뿐 아니라 현재 재고도 전달합니다. 따라서 Cart는 장바구니 수량과 Dish 재고를 비교해 주문 가능 여부를 갱신할 수 있습니다.
 */
public record DishStateChangedEvent(
    UUID eventId,
    int schemaVersion,
    Long dishId,
    long aggregateVersion,
    DishStateChangedPayload payload,
    Instant occurredAt)
    implements DomainEvent<DishStateChangedPayload> {

  public static final String EVENT_TYPE = "DISH_STATE_CHANGED";

  public static final int SCHEMA_VERSION = 2;

  @Override
  public String eventType() {
    return EVENT_TYPE;
  }

  @Override
  public String aggregateType() {
    return "DISH";
  }

  @Override
  public Long aggregateId() {
    return dishId;
  }
}
