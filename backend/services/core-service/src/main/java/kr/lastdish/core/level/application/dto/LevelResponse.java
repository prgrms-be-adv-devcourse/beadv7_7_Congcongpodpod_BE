package kr.lastdish.core.level.application.dto;

import java.math.BigDecimal;
import kr.lastdish.core.level.domain.DishLevel;
import kr.lastdish.core.level.domain.Level;

public record LevelResponse(
    DishLevel dishLevel,
    int purchaseCount,
    BigDecimal discountAmount,
    int remainToNextLevel // 다음 등급까지 남은 구매횟수 (최고 등급이면 0)
    ) {

  public static LevelResponse from(Level level) {
    DishLevel nextLevel = level.getDishLevel().next();
    int remaining = nextLevel == null ? 0 : nextLevel.getMinPurchases() - level.getPurchaseCount();

    return new LevelResponse(
        level.getDishLevel(), level.getPurchaseCount(), level.getDiscountAmount(), remaining);
  }
}
