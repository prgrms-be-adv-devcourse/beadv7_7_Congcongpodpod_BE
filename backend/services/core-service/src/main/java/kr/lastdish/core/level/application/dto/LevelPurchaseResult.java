package kr.lastdish.core.level.application.dto;

import kr.lastdish.core.level.domain.DishLevel;

public record LevelPurchaseResult(boolean upgraded, DishLevel currentLevel) {}
