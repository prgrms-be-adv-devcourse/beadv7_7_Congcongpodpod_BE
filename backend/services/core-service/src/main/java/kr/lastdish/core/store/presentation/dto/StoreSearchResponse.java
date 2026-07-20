package kr.lastdish.core.store.presentation.dto;

import java.util.List;
import kr.lastdish.core.store.application.dto.StorePageResult;

public record StoreSearchResponse(
    List<StoreResponse> stores, int page, int size, long totalElements, int totalPages) {

  public static StoreSearchResponse from(StorePageResult result) {
    List<StoreResponse> stores = result.stores().stream().map(StoreResponse::from).toList();

    return new StoreSearchResponse(
        stores, result.page(), result.size(), result.totalElements(), result.totalPages());
  }
}
