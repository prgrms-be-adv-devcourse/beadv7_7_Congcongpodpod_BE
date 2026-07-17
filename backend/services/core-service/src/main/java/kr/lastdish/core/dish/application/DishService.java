package kr.lastdish.core.dish.application;

import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import kr.lastdish.core.dish.presentation.dto.DishCreateRequest;
import kr.lastdish.core.dish.presentation.dto.DishCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DishService {
    private final DishRepository dishRepository;

    @Transactional
    public DishCreateResponse createDish(DishCreateRequest request) {
        Dish dish = Dish.create(
                request.storeId(),
                request.dishName(),
                request.registeredAt(),
                request.description(),
                request.category(),
                request.thumbnailUrl(),
                request.stockQuantity(),
                request.dishPrice(),
                request.discountPrice()
        );

        Dish savedDish = dishRepository.save(dish);

        return DishCreateResponse.from(savedDish);
    }
}
