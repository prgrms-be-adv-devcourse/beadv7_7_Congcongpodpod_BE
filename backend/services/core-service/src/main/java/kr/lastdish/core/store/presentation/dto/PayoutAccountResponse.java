package kr.lastdish.core.store.presentation.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import kr.lastdish.core.store.application.dto.PayoutAccountResult;

import java.time.LocalDateTime;

public record PayoutAccountResponse(
        Long payoutAccountId,
        Long storeId,
        String accountNumber,
        String accountHolder,
        boolean active,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {
        public static PayoutAccountResponse from(PayoutAccountResult result) {
                return new PayoutAccountResponse(
                        result.payoutAccountId(),
                        result.storeId(),
                        result.accountNumber(),
                        result.accountHolder(),
                        result.active(),
                        result.updatedAt()
                );
        }
}
