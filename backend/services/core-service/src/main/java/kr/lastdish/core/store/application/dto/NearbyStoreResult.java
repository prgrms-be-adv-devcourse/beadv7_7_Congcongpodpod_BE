package kr.lastdish.core.store.application.dto;

import java.util.List;
import kr.lastdish.core.dish.presentation.dto.DishResponse;

public record NearbyStoreResult(StoreResult store, List<DishResponse> dishes) {}
