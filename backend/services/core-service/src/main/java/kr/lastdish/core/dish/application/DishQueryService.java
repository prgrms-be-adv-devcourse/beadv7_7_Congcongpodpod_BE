package kr.lastdish.core.dish.application;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kr.lastdish.core.dish.application.dto.DishQuerySnapshot;
import kr.lastdish.core.dish.application.port.in.DishQueryUseCase;
import kr.lastdish.core.dish.domain.Dish;
import kr.lastdish.core.dish.domain.DishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DishQueryService implements DishQueryUseCase {

  private final DishRepository dishRepository;

  @Override
  public Map<Long, List<DishQuerySnapshot>> findOnSaleByStoreIds(List<Long> storeIds) {
    if (storeIds.isEmpty()) {
      return Map.of();
    }
    return dishRepository.findOnSaleByStoreIds(storeIds).stream()
        .map(this::toSnapshot)
        .collect(Collectors.groupingBy(DishQuerySnapshot::storeId));
  }

  private DishQuerySnapshot toSnapshot(Dish dish) {
    return new DishQuerySnapshot(
        dish.getId(),
        dish.getStoreId(),
        dish.getDishName(),
        dish.getRegisteredAt(),
        dish.getDescription(),
        dish.getThumbnailUrl(),
        dish.getStockQuantity(),
        dish.getDishPrice(),
        dish.getDiscountPrice());
  }
}
