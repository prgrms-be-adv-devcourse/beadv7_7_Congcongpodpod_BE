package kr.lastdish.core.settlement.application.dto;

import java.time.LocalDateTime;
import java.time.YearMonth;

public record SettlementPeriod(
        YearMonth settlementMonth,
        LocalDateTime periodStart,
        LocalDateTime periodEnd
) {
    public static SettlementPeriod from(YearMonth settlementMonth){
        return new SettlementPeriod(
                settlementMonth,
                settlementMonth.atDay(1).atStartOfDay(),
                settlementMonth.plusMonths(1).atDay(1).atStartOfDay()
        );
    }
}
