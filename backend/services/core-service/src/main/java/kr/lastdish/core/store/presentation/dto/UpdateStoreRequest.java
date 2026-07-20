package kr.lastdish.core.store.presentation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import kr.lastdish.core.store.application.dto.UpdateStoreCommand;

public record UpdateStoreRequest(
    @NotBlank String storeName,
    @NotBlank String storeAddress,
    @NotBlank String storePhone,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime openTime,
    @NotNull @JsonFormat(pattern = "HH:mm") LocalTime closeTime,
    @NotNull BigDecimal latitude,
    @NotNull BigDecimal longitude,
    List<DayOfWeek> holidays) {

  public UpdateStoreCommand toCommand() {
    return new UpdateStoreCommand(
        storeName,
        storeAddress,
        storePhone,
        openTime,
        closeTime,
        latitude,
        longitude,
        holidays == null ? List.of() : holidays);
  }
}
