package kr.lastdish.core.dish.infrastructure;

import java.util.Optional;
import kr.lastdish.core.dish.domain.Dish;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DishJpaRepository extends JpaRepository<Dish, Long> {
  Optional<Dish> findByIdAndIsDeletedFalse(Long dishId);
}
