package kr.lastdish.core.settlement.presentation.dto;

import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;

public record MonthlySettlementJobRequest(
    @NotNull(message = "정산 월은 필수입니다.") YearMonth settlementMonth) {}
