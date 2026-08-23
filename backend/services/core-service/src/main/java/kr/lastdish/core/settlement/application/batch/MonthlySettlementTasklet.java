package kr.lastdish.core.settlement.application.batch;

import java.time.YearMonth;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.settlement.application.SettlementEventService;
import kr.lastdish.core.settlement.application.SettlementService;
import kr.lastdish.core.settlement.application.dto.SettlementProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlySettlementTasklet implements Tasklet {
  private final SettlementService settlementService;
  private final SettlementEventService settlementEventService;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    String monthValue =
        contribution.getStepExecution().getJobParameters().getString("settlementMonth");

    if (monthValue == null || monthValue.isBlank()) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "settlementMonth는 필수입니다.");
    }

    YearMonth settlementMonth = YearMonth.parse(monthValue);

    List<Long> targetStoreIds =
        settlementEventService.findMonthlySettlementTargetStoreIds(settlementMonth);

    int createdCount = 0;
    int retriedCount = 0;
    int skippedCount = 0;
    int failedCount = 0;

    for (Long storeId : targetStoreIds) {
      try {
        SettlementProcessResult result =
            settlementEventService.processMonthlySettlement(storeId, settlementMonth);

        switch (result.status()) {
          case CREATED -> createdCount++;
          case RETRIED -> retriedCount++;
          case SKIPPED -> skippedCount++;
          case FAILED -> failedCount++;
        }

        log.info(
            "매장별 월 정산 처리 결과. storeId={}, settlementId={}, settlementMonth={}, status={}, message={}",
            result.storeId(),
            result.settlementId(),
            settlementMonth,
            result.status(),
            result.message());
      } catch (RuntimeException exception) {
        failedCount++;

        log.error(
            "월 정산 처리 중 처리되지 않은 오류 발생. storeId={}, settlementMonth={}",
            storeId,
            settlementMonth,
            exception);
      }
    }
    var context = contribution.getStepExecution().getExecutionContext();

    context.putInt("targetStoreCount", targetStoreIds.size());
    context.putInt("createdStoreCount", createdCount);
    context.putInt("retriedStoreCount", retriedCount);
    context.putInt("skippedStoreCount", skippedCount);
    context.putInt("failedStoreCount", failedCount);

    log.info(
        "월 정산 배치 완료. settlementMonth={}, targetCount={}, createdCount={}, retriedCount={}, skippedCount={}, failedCount={}",
        settlementMonth,
        targetStoreIds.size(),
        createdCount,
        retriedCount,
        skippedCount,
        failedCount);

    return RepeatStatus.FINISHED;
  }
}
