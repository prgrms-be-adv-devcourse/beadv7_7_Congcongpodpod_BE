package kr.lastdish.core.dish.domain;

public interface DishRepository {
  Dish save(Dish dish);

  Dish findById(Long dishId);
}
