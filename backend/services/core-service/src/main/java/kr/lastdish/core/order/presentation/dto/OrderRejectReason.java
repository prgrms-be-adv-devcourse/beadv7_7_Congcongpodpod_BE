package kr.lastdish.core.order.presentation.dto;

public enum OrderRejectReason {
  OUT_OF_STOCK(false, "재고 소진으로 주문이 취소되었어요. 결제하신 금액은 예치금으로 환불됩니다."),
  QUALITY_ISSUE(false, "상품 준비 중 문제가 발견되어 주문이 취소되었어요. 결제하신 금액은 예치금으로 환불됩니다."),
  NOT_READY(true, "매장 사정으로 준비가 어려워 주문이 취소되었어요. 결제하신 금액은 예치금으로 환불됩니다."),
  STORE_CLOSED(false, "매장 사정으로 오늘 픽업이 어려워 주문이 취소되었어요. 결제하신 금액은 예치금으로 환불됩니다."),
  SYSTEM_ERROR(true, "일시적인 오류로 주문이 취소되었어요. 결제하신 금액은 예치금으로 환불됩니다. 불편을 드려 죄송합니다.");

  private final boolean restoreStock;
  private final String message;

  OrderRejectReason(boolean restoreStock, String message) {
    this.restoreStock = restoreStock;
    this.message = message;
  }

  public boolean shouldRestoreStock() {
    return restoreStock;
  }

  public String getMessage() {
    return message;
  }
}
