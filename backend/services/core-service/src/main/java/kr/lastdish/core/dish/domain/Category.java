package kr.lastdish.core.dish.domain;

public enum Category {
  CHICKEN("치킨"),
  CHINESE("중식"),
  BUNSIK("분식"),
  KOREAN("한식"),
  SOUP_STEW("찜·탕"),
  CUTLET_SUSHI("돈까스·회"),
  PIZZA("피자"),
  CAFE_DESSERT("카페·디저트"),
  FAST_FOOD("패스트푸드"),
  JOKBAL_BOSSAM("족발·보쌈"),
  MEAT("고기"),
  LATE_NIGHT("야식"),
  WESTERN("양식"),
  ASIAN("아시안"),
  LUNCH_BOX("도시락");

  private final String displayName;

  Category(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
