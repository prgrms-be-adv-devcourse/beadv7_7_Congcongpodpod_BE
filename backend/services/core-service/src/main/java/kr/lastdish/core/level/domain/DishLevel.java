package kr.lastdish.core.level.domain;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DishLevel {
  LEVEL_1(1, 0, new BigDecimal("0.05")), // Lv.1 양념 종지 (0회 이상 - 5회 미만 / 5%)
  LEVEL_2(2, 5, new BigDecimal("0.06")), // Lv.2 밥그릇 (5회 이상 - 10회 미만 / 6%)
  LEVEL_3(3, 10, new BigDecimal("0.07")), // Lv.3 뚝배기 (10회 이상 - 15회 미만 / 7%)
  LEVEL_4(4, 15, new BigDecimal("0.08")), // Lv.4 전골냄비 (15회 이상 - 20회 미만 / 8%)
  LEVEL_5(5, 20, new BigDecimal("0.10")); // Lv.5 가마솥 (20회 이상 / 10%)

  private final int levelNum;
  private final int minPurchases;
  private final BigDecimal pointPercent;

  public static DishLevel fromPurchaseCount(int purchaseCount) {
    if (purchaseCount >= LEVEL_5.minPurchases) return LEVEL_5;
    if (purchaseCount >= LEVEL_4.minPurchases) return LEVEL_4;
    if (purchaseCount >= LEVEL_3.minPurchases) return LEVEL_3;
    if (purchaseCount >= LEVEL_2.minPurchases) return LEVEL_2;
    return LEVEL_1;
  }

  public DishLevel next() {
    DishLevel[] levels = values();
    int nextIndex = ordinal() + 1;
    return nextIndex < levels.length ? levels[nextIndex] : null;
  }
}
