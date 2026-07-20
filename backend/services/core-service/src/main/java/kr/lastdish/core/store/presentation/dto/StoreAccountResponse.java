package kr.lastdish.core.store.presentation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import kr.lastdish.core.store.application.dto.PayoutAccountResult;

public record StoreAccountResponse(
    Long payoutAccountId,
    Long storeId,
    String accountNumber,
    String accountHolder,
    boolean active,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime updatedAt) {
  public static StoreAccountResponse from(PayoutAccountResult result) {
    return new StoreAccountResponse(
        result.payoutAccountId(),
        result.storeId(),
        result.accountNumber(),
        result.accountHolder(),
        result.active(),
        result.updatedAt());
  }
}
