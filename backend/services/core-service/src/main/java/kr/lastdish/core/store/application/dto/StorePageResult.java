package kr.lastdish.core.store.application.dto;

import java.util.List;

public record StorePageResult(
    List<NearbyStoreResult> stores, int page, int size, long totalElements, int totalPages) {

  public static StorePageResult of(
      List<NearbyStoreResult> stores, int page, int size, long totalElements) {
    int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

    return new StorePageResult(stores, page, size, totalElements, totalPages);
  }
}
