package kr.lastdish.core.dish.domain;

public interface DishRepository {
  Dish save(Dish dish);

  Dish findById(Long dishId);

  Dish findByIdAndIsDeletedFalse(Long dishId);

  Dish findWithLockByIdAndIsDeletedFalse(Long dishId);
}
