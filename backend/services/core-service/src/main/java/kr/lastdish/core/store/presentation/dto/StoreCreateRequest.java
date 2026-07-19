package kr.lastdish.core.store.presentation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import kr.lastdish.core.store.application.dto.RegisterStoreCommand;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record StoreCreateRequest(
        @NotBlank
        String storeName,

        @NotBlank
        String businessNumber,

        @NotBlank
        String storeAddress,

        @NotBlank
        String storePhone,

        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime openTime,

        @NotNull
        @JsonFormat(pattern = "HH:mm")
        LocalTime closeTime,

        @NotNull
        BigDecimal latitude,

        @NotNull
        BigDecimal longitude,

        List<DayOfWeek> holidays
) {

    public RegisterStoreCommand toCommand(
            Long memberId
    ) {
        return new RegisterStoreCommand(
                memberId,
                storeName,
                businessNumber,
                storeAddress,
                storePhone,
                openTime,
                closeTime,
                latitude,
                longitude,
                holidays == null ? List.of() : holidays
        );
    }
}
