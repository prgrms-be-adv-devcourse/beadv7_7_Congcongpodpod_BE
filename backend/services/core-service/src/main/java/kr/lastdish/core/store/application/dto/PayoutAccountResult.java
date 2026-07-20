package kr.lastdish.core.store.application.dto;

import java.time.LocalDateTime;
import kr.lastdish.core.store.domain.StorePayoutAccount;

public record PayoutAccountResult(
    Long payoutAccountId,
    Long storeId,
    String accountNumber,
    String accountHolder,
    boolean active,
    LocalDateTime updatedAt) {
  public static PayoutAccountResult from(StorePayoutAccount account) {
    return new PayoutAccountResult(
        account.getId(),
        account.getStoreId(),
        maskAccountNumber(account.getAccountNumber()),
        account.getAccountHolder(),
        account.isActive(),
        account.getUpdatedAt());
  }

  // 응답 시 마스킹해서 반환
  private static String maskAccountNumber(String accountNumber) {
    if (accountNumber == null || accountNumber.length() <= 4) {
      return "****";
    }

    int visibleLength = 4;

    return "*".repeat(accountNumber.length() - visibleLength)
        + accountNumber.substring(accountNumber.length() - visibleLength);
  }
}
