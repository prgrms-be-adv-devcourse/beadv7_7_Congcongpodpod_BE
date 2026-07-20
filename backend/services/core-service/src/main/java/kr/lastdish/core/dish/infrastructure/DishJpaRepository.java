package kr.lastdish.core.dish.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import kr.lastdish.core.dish.domain.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DishJpaRepository extends JpaRepository<Dish, Long> {
  Optional<Dish> findByIdAndIsDeletedFalse(Long dishId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Dish> findWithLockByIdAndIsDeletedFalse(Long dishId);
}
