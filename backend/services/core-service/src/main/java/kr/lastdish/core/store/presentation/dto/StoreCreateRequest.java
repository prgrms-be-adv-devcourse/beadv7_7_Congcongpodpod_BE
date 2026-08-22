package kr.lastdish.core.store.presentation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.store.application.dto.RegisterStoreCommand;
import kr.lastdish.core.store.domain.Category;

public record StoreCreateRequest(
    @NotBlank String storeName,
    @NotBlank String businessNumber,
    @NotBlank String storeAddress,
    String storeDetailAddress,
    @NotBlank String storePhone,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime openTime,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime closeTime,
    @NotNull BigDecimal latitude,
    @NotNull BigDecimal longitude,
    @NotNull Category category,
    List<DayOfWeek> holidays) {

  public RegisterStoreCommand toCommand(Long memberId) {
    return new RegisterStoreCommand(
        memberId,
        storeName,
        businessNumber,
        storeAddress,
        storeDetailAddress,
        storePhone,
        openTime,
        closeTime,
        latitude,
        longitude,
        category,
        holidays == null ? List.of() : holidays);
  }
}
