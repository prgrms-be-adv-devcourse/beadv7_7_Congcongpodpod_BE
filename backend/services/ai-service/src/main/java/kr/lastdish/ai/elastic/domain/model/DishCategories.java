package kr.lastdish.ai.elastic.domain.model;

import java.util.List;

/** dishes.category에 색인되는 값과 정확히 일치해야 하는 카테고리 목록. ES term filter가 exact match */
public final class DishCategories {

  public static final List<String> ALL =
      List.of("식사빵", "디저트 빵", "케이크", "디저트", "샐러드", "샌드위치", "밥류", "음료 / 카페", "과일류", "유제품");

  private DishCategories() {}
}
