package kr.lastdish.core.store.domain.event;

import kr.lastdish.core.store.domain.StoreStatus;

public record StoreStatusChangedPayload(Long storeId, StoreStatus status) {
}
