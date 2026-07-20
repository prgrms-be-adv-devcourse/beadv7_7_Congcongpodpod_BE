package kr.lastdish.core.store.presentation.dto;

import jakarta.validation.constraints.NotNull;
import kr.lastdish.core.store.domain.StoreStatus;

public record ChangeStoreStatusRequest(@NotNull StoreStatus status) {}
