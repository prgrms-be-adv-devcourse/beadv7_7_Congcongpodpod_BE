package kr.lastdish.core.dish.infrastructure;

import kr.lastdish.core.dish.domain.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DishJpaRepository extends JpaRepository<Dish, Long> {
    Optional<Dish> findByIdAndIsDeletedFalse(Long dishId);
}
