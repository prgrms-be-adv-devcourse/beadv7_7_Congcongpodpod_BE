package kr.lastdish.core.dish.domain;

public enum DishStatus {
  ON_SALE("판매중"),
  SOLD_OUT("품절"),
  CLOSED("마감"),
  EXPIRED("만료");

  private final String displayName;

  DishStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
