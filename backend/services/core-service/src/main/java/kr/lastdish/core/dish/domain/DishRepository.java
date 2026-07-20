package kr.lastdish.core.dish.domain;

import java.util.Optional;

public interface DishRepository {
  Dish save(Dish dish);

  Dish findById(Long dishId);

  Dish findByIdAndIsDeletedFalse(Long dishId);

  Optional<Dish> findAvailableById(Long dishId);
}
