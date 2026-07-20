package kr.lastdish.core.dish.domain;

import java.util.Optional;

public interface DishRepository {
  Dish save(Dish dish);

  Optional<Dish> findById(Long dishId);

  Dish findByIdAndIsDeletedFalse(Long dishId);
}
