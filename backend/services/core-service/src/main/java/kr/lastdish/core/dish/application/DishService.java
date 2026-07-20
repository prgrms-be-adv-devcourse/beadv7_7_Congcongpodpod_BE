package kr.lastdish.core.dish.application;

import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.presentation.dto.DIshUpdateRequest;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DishService {
  private final DishRepository dishRepository;

  @Transactional
  public DishResponse createDish(DishCreateRequest request) {
    Dish dish =
        Dish.create(
            request.storeId(),
            request.dishName(),
            request.registeredAt(),
            request.description(),
            request.category(),
            request.thumbnailUrl(),
            request.stockQuantity(),
            request.dishPrice(),
            request.discountPrice());

    Dish savedDish = dishRepository.save(dish);
    return DishResponse.from(savedDish);
  }

  @Transactional
  public DishResponse updateDish(Long dishId, DIshUpdateRequest request) {
    Dish dish = getDish(dishId);
    dish.update(
        request.dishName(),
        request.registeredAt(),
        request.description(),
        request.category(),
        request.thumbnailUrl(),
        request.stockQuantity(),
        request.dishPrice(),
        request.discountPrice());
    return DishResponse.from(dish);
  }

  @Transactional
  public void deleteDish(Long dishId) {
    Dish dish = getDish(dishId);
    dish.delete();
  }

  public DishResponse getEachDish(Long dishId) {
    Dish dish = getDish(dishId);
    return DishResponse.from(dish);
  }

  private Dish getDish(Long dishId) {
    return dishRepository.findById(dishId);
  }
}
