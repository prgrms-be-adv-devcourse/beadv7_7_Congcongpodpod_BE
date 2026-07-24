package kr.lastdish.core.settlement.presentation.dto;

import org.springframework.batch.core.job.JobExecution;

import java.time.YearMonth;

public record MonthlySettlementJobResponse(
        long jobExecutionId,
        String jobName,
        YearMonth settlementMonth,
        String status
) {

    public static MonthlySettlementJobResponse from(
            JobExecution jobExecution,
            YearMonth settlementMonth
    ) {
        return new MonthlySettlementJobResponse(
                jobExecution.getId(),
                jobExecution.getJobInstance().getJobName(),
                settlementMonth,
                jobExecution.getStatus().name()
        );
    }
}
