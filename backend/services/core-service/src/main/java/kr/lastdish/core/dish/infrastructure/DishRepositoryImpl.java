package kr.lastdish.core.dish.infrastructure;

import java.util.List;
import java.util.Optional;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.core.common.exception.ErrorCode;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.domain.DishStatus;
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
        .orElseThrow(() -> new BusinessException(ErrorCode.DISH_NOT_FOUND));
  }

  @Override
  public Dish findByIdAndIsDeletedFalse(Long dishId) {
    return dishJpaRepository
        .findByIdAndIsDeletedFalse(dishId)
        .orElseThrow(() -> new BusinessException(ErrorCode.DISH_NOT_FOUND));
  }

  @Override
  public Dish findWithLockByIdAndIsDeletedFalse(Long dishId) {
    return dishJpaRepository
        .findWithLockByIdAndIsDeletedFalse(dishId)
        .orElseThrow(() -> new BusinessException(ErrorCode.DISH_NOT_FOUND));
  }

  @Override
  public Optional<Dish> findAvailableById(Long dishId) {
    return dishJpaRepository.findByIdAndIsDeletedFalse(dishId);
  }

  @Override
  public boolean existsByStoreIdAndIsDeletedFalse(Long storeId) {
    return dishJpaRepository.existsByStoreIdAndIsDeletedFalse(storeId);
  }

  @Override
  public List<Dish> findOnSaleByStoreId(Long storeId) {
    return dishJpaRepository.findOnSaleByStoreId(storeId, DishStatus.ON_SALE);
  }

  @Override
  public List<Dish> findOnSaleByStoreIds(List<Long> storeIds) {
    return dishJpaRepository.findOnSaleByStoreIds(storeIds, DishStatus.ON_SALE);
  }

  @Override
  public Optional<Dish> findByStoreIdAndIsDeletedFalse(Long storeId) {
    return dishJpaRepository.findByStoreIdAndIsDeletedFalse(storeId);
  }

  @Override
  public Optional<Dish> findWithLockByStoreIdAndIsDeletedFalse(Long storeId) {
    return dishJpaRepository.findWithLockByStoreIdAndIsDeletedFalse(storeId);
  }
}
