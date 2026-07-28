package kr.lastdish.core.dish.infrastructure;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DishJpaRepository extends JpaRepository<Dish, Long> {
  Optional<Dish> findByIdAndIsDeletedFalse(Long dishId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Dish> findWithLockByIdAndIsDeletedFalse(Long dishId);

  boolean existsByStoreIdAndIsDeletedFalse(Long storeId);

  Optional<Dish> findByStoreIdAndIsDeletedFalse(Long storeId);

  @Query(
      """
    SELECT d
    FROM Dish d
    WHERE d.storeId = :storeId
        AND d.isDeleted = false
        AND d.dishStatus = :status
    """)
  List<Dish> findOnSaleByStoreId(
      @Param("storeId") Long storeId, @Param("status") DishStatus status);
}
