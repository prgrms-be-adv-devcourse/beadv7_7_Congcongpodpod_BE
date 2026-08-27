package kr.lastdish.core.settlement.application.batch;

import kr.lastdish.core.settlement.application.dto.SettlementProcessResult;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
public class MonthlySettlementItemWriter
    implements ItemWriter<SettlementProcessResult>, StepExecutionListener {
  private int targetCount;
  private int createdCount;
  private int retriedCount;
  private int skippedCount;
  private int failedCount;

  @Override
  public void write(Chunk<? extends SettlementProcessResult> items) throws Exception {
    for (SettlementProcessResult result : items) {
      targetCount++;

      switch (result.status()) {
        case CREATED -> createdCount++;
        case RETRIED -> retriedCount++;
        case SKIPPED -> skippedCount++;
        case FAILED -> failedCount++;
      }

      log.info(
          "매장별 월 정산 처리 결과. storeId={}, settlementId={}, status={}, message={}",
          result.storeId(),
          result.settlementId(),
          result.status(),
          result.message());
    }
  }

  @Override
  public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
    var context = stepExecution.getExecutionContext();
    context.putInt("targetStoreCount", targetCount);
    context.putInt("createdStoreCount", createdCount);
    context.putInt("retriedStoreCount", retriedCount);
    context.putInt("skippedStoreCount", skippedCount);
    context.putInt("failedStoreCount", failedCount);

    log.info(
        "월 정산 배치 완료. settlementMonth={}, targetCount={}, createdCount={}, retriedCount={}, skippedCount={}, failedCount={}",
        stepExecution.getJobParameters().getString("settlementMonth"),
        targetCount,
        createdCount,
        retriedCount,
        skippedCount,
        failedCount);

    return stepExecution.getExitStatus();
  }
}
