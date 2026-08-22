package kr.lastdish.core.favorite.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record FavoriteAddRequest(@NotNull Long storeId) {}
