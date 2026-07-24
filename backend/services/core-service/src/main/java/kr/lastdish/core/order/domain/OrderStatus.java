package kr.lastdish.core.order.domain;

public enum OrderStatus {
  RESERVED("예약 완료"),
  PICKUP_READY("픽업 대기"),
  PICKED_UP("픽업 완료"),
  NO_SHOW("노쇼"),
  CANCELLED("취소"),
  REJECTED("거절");

  private final String displayName;

  OrderStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public boolean canTransitionTo(OrderStatus nextStatus) {
    return switch (this) {
      case RESERVED ->
          nextStatus == PICKUP_READY || nextStatus == CANCELLED || nextStatus == REJECTED;
      case PICKUP_READY -> nextStatus == PICKED_UP || nextStatus == NO_SHOW;
      case PICKED_UP, NO_SHOW, CANCELLED, REJECTED -> false;
    };
  }
}
