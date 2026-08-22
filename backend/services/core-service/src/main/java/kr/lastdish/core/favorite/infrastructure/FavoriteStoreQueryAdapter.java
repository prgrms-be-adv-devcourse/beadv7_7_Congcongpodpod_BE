package kr.lastdish.core.favorite.infrastructure;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import kr.lastdish.core.dish.application.dto.DishQuerySnapshot;
import kr.lastdish.core.dish.application.port.in.DishQueryUseCase;
import kr.lastdish.core.favorite.application.dto.FavoriteDishResult;
import kr.lastdish.core.favorite.application.dto.FavoriteStoreResult;
import kr.lastdish.core.favorite.application.port.out.FavoriteStoreQueryPort;
import kr.lastdish.core.store.application.dto.StoreQuerySnapshot;
import kr.lastdish.core.store.application.port.in.StoreQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoriteStoreQueryAdapter implements FavoriteStoreQueryPort {

  private final StoreQueryUseCase storeQueryUseCase;
  private final DishQueryUseCase dishQueryUseCase;

  @Override
  public boolean existsById(Long storeId) {
    return storeQueryUseCase.existsActiveStore(storeId);
  }

  @Override
  public List<FavoriteStoreResult> findByIds(List<Long> storeIds) {
    if (storeIds.isEmpty()) {
      return List.of();
    }

    Map<Long, StoreQuerySnapshot> storesById =
        storeQueryUseCase.findActiveStores(storeIds).stream()
            .collect(Collectors.toMap(StoreQuerySnapshot::storeId, Function.identity()));
    Map<Long, List<DishQuerySnapshot>> dishesByStoreId =
        dishQueryUseCase.findOnSaleByStoreIds(storeIds);

    return storeIds.stream()
        .map(storesById::get)
        .filter(Objects::nonNull)
        .map(store -> toResult(store, dishesByStoreId.getOrDefault(store.storeId(), List.of())))
        .toList();
  }

  private FavoriteStoreResult toResult(
      StoreQuerySnapshot store, List<DishQuerySnapshot> dishSnapshots) {
    List<FavoriteDishResult> dishes =
        dishSnapshots.stream()
            .map(
                dish ->
                    new FavoriteDishResult(
                        dish.dishId(),
                        dish.dishName(),
                        dish.registeredAt(),
                        dish.description(),
                        dish.thumbnailUrl(),
                        dish.stockQuantity(),
                        dish.dishPrice(),
                        dish.discountPrice()))
            .toList();

    return new FavoriteStoreResult(
        store.storeId(),
        store.memberId(),
        store.storeName(),
        store.businessNumber(),
        store.storeAddress(),
        store.storePhone(),
        store.openTime(),
        store.closeTime(),
        store.status(),
        store.latitude(),
        store.longitude(),
        store.category(),
        store.holidays(),
        dishes);
  }
}
