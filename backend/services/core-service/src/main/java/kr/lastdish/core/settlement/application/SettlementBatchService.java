package kr.lastdish.core.settlement.application;

import java.time.YearMonth;
import kr.lastdish.core.settlement.presentation.dto.MonthlySettlementJobResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SettlementBatchService {

  private final JobOperator jobOperator;
  private final Job monthlySettlementJob;

  public MonthlySettlementJobResponse runMonthlySettlement(YearMonth settlementMonth) {
    JobParameters jobParameters =
        new JobParametersBuilder()
            .addString("settlementMonth", settlementMonth.toString())
            .addLong("requestedAt", System.currentTimeMillis())
            .toJobParameters();

    try {
      JobExecution jobExecution = jobOperator.start(monthlySettlementJob, jobParameters);

      return MonthlySettlementJobResponse.from(jobExecution, settlementMonth);
    } catch (Exception exception) {
      throw new IllegalStateException("월 정산 배치를 실행하지 못했습니다.", exception);
    }
  }
}
