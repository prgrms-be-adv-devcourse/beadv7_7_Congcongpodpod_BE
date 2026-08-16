package kr.lastdish.core.point.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import kr.lastdish.core.point.domain.PointHistory;
import kr.lastdish.core.point.domain.PointType;

public record PointTransactionResult(
        Long historyId,
        PointType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        LocalDateTime createdAt) {

    public static PointTransactionResult from(PointHistory history) {
        return new PointTransactionResult(
                history.getId(),
                history.getType(),
                history.getAmount(),
                history.getBalanceAfter(),
                history.getCreatedAt());
    }
}