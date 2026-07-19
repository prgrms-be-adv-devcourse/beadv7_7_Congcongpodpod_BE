package kr.lastdish.core.dish.infrastructure;

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
}
