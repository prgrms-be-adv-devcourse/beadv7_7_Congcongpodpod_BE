package kr.lastdish.core.order.presentation.dto;

public enum OrderRejectReason {
  OUT_OF_STOCK(false),
  QUALITY_ISSUE(false),
  NOT_READY(true),
  STORE_CLOSED(false),
  SYSTEM_ERROR(true);

  private final boolean restoreStock;

  OrderRejectReason(boolean restoreStock) {
    this.restoreStock = restoreStock;
  }

  public boolean shouldRestoreStock() {
    return restoreStock;
  }
}
