package kr.lastdish.core.store.presentation.dto;

import java.util.List;
import kr.lastdish.core.store.application.dto.StorePageResult;

public record StoreSearchResponse(
    List<NearbyStoreResponse> stores, int page, int size, long totalElements, int totalPages) {

  public static StoreSearchResponse from(StorePageResult result) {
    List<NearbyStoreResponse> stores =
        result.stores().stream().map(NearbyStoreResponse::from).toList();

    return new StoreSearchResponse(
        stores, result.page(), result.size(), result.totalElements(), result.totalPages());
  }
}
