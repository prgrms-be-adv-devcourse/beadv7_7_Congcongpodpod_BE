package kr.lastdish.core.store.application.dto;

import kr.lastdish.core.store.domain.Store;

import java.util.List;

public record StorePageResult(
        List<StoreResult> stores,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static StorePageResult of(
            List<Store> stores,
            int page,
            int size,
            long totalElements
    ) {
        List<StoreResult> results = stores.stream()
                .map(StoreResult::from)
                .toList();

        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil(
                (double) totalElements / size
        );

        return new StorePageResult(
                results,
                page,
                size,
                totalElements,
                totalPages
        );
    }
}