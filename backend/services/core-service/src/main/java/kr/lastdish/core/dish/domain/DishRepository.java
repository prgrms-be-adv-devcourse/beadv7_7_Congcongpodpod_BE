package kr.lastdish.core.dish.domain;

import java.util.List;
import java.util.Optional;

public interface DishRepository {
  Dish save(Dish dish);

  Dish findById(Long dishId);

  Dish findByIdAndIsDeletedFalse(Long dishId);

  Dish findWithLockByIdAndIsDeletedFalse(Long dishId);

  Optional<Dish> findAvailableById(Long dishId);

  boolean existsByStoreIdAndIsDeletedFalse(Long storeId);

  List<Dish> findOnSaleByStoreId(Long storeId);

  List<Dish> findAllByStoreIdAndIsDeletedFalse(Long storeId);

  List<Dish> findOnSaleByStoreIds(List<Long> storeIds);

  Optional<Dish> findByStoreIdAndIsDeletedFalse(Long storeId);

  Optional<Dish> findWithLockByStoreIdAndIsDeletedFalse(Long storeId);
}
