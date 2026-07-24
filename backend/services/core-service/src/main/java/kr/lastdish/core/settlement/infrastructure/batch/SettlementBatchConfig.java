package kr.lastdish.core.settlement.infrastructure.batch;

import kr.lastdish.core.settlement.application.batch.MonthlySettlementTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SettlementBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MonthlySettlementTasklet monthlySettlementTasklet;

    @Bean
    public Job monthlySettlementJob(){
        return new JobBuilder("monthlySettlementJob", jobRepository)
                .start(monthlySettlementStep())
                .build();
    }

    @Bean
    public Step monthlySettlementStep(){
        return new StepBuilder("monthlySettlementStep", jobRepository)
                .tasklet(monthlySettlementTasklet)
                .transactionManager(transactionManager)
                .build();
    }
}
