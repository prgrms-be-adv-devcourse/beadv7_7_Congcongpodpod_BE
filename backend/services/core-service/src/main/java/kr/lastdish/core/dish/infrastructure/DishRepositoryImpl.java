package kr.lastdish.core.dish.infrastructure;

import java.util.Optional;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DishRepositoryImpl implements DishRepository {
  private final DishJpaRepository dishJpaRepository;

  @Override
  public Dish save(Dish dish) {
    return dishJpaRepository.save(dish);
  }

  @Override
  public Dish findById(Long dishId) {
    return dishJpaRepository
        .findByIdAndIsDeletedFalse(dishId)
        .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));
  }

  @Override
  public Dish findByIdAndIsDeletedFalse(Long dishId) {
    return dishJpaRepository
        .findByIdAndIsDeletedFalse(dishId)
        .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));
  }

  @Override
  public Dish findWithLockByIdAndIsDeletedFalse(Long dishId) {
    return dishJpaRepository.findWithLockByIdAndIsDeletedFalse(dishId).orElseThrow();
  }

  @Override
  public Optional<Dish> findAvailableById(Long dishId) {
    return dishJpaRepository.findByIdAndIsDeletedFalse(dishId);
  }
}
