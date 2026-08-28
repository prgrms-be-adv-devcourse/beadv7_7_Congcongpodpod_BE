package kr.lastdish.core.settlement.infrastructure.batch;

import kr.lastdish.core.settlement.application.batch.MonthlySettlementItemProcessor;
import kr.lastdish.core.settlement.application.batch.MonthlySettlementItemWriter;
import kr.lastdish.core.settlement.application.dto.SettlementProcessResult;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableScheduling
public class SettlementBatchConfig {
  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final MonthlySettlementTargetReader monthlySettlementTargetReader;
  private final MonthlySettlementItemProcessor monthlySettlementItemProcessor;
  private final MonthlySettlementItemWriter monthlySettlementItemWriter;

  public SettlementBatchConfig(
          JobRepository jobRepository,
          @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
          MonthlySettlementTargetReader monthlySettlementTargetReader,
          MonthlySettlementItemProcessor monthlySettlementItemProcessor,
          MonthlySettlementItemWriter monthlySettlementItemWriter) {
    this.jobRepository = jobRepository;
    this.transactionManager = transactionManager;
    this.monthlySettlementTargetReader = monthlySettlementTargetReader;
    this.monthlySettlementItemProcessor = monthlySettlementItemProcessor;
    this.monthlySettlementItemWriter = monthlySettlementItemWriter;
  }

  @Bean
  public Job monthlySettlementJob() {
    return new JobBuilder("monthlySettlementJob", jobRepository)
            .start(monthlySettlementStep())
            .build();
  }

  @Bean
  public Step monthlySettlementStep() {
    return new StepBuilder("monthlySettlementStep", jobRepository)
            .<Long, SettlementProcessResult>chunk(1)
            .transactionManager(transactionManager)
            .reader(monthlySettlementTargetReader)
            .processor(monthlySettlementItemProcessor)
            .writer(monthlySettlementItemWriter)
            .listener(monthlySettlementItemWriter)
            .build();
  }
}
