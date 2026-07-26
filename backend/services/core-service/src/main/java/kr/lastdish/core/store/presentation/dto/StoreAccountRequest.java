package kr.lastdish.core.store.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record StoreAccountRequest(@NotBlank String bankName, @NotBlank String accountNumber, @NotBlank String accountHolder) {}
