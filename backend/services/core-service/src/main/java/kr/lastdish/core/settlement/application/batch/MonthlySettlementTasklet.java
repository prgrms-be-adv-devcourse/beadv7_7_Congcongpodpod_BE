package kr.lastdish.core.settlement.application.batch;

import java.time.YearMonth;
import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.settlement.application.SettlementService;
import kr.lastdish.core.settlement.application.SettlementStoreReader;
import kr.lastdish.core.settlement.application.dto.SettlementCreateResult;
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
  private final SettlementStoreReader settlementStoreReader;
  private final SettlementService settlementService;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    String monthValue =
        contribution.getStepExecution().getJobParameters().getString("settlementMonth");

    if (monthValue == null || monthValue.isBlank()) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "settlementMonth는 필수입니다.");
    }

    YearMonth settlementMonth = YearMonth.parse(monthValue);

    List<Long> storeIds = settlementStoreReader.readSettlementTargetStoreIds();

    int createdCount = 0;
    int skippedCount = 0;
    int failedCount = 0;

    for (Long storeId : storeIds) {
      try {
        SettlementCreateResult result =
            settlementService.createMonthlySettlement(storeId, settlementMonth);

        if (result.created()) {
          createdCount++;
        } else {
          skippedCount++;
        }
      } catch (RuntimeException exception) {
        failedCount++;
        log.error(
            "월 정산 생성 실패. storeId={}, settlementMonth={}", storeId, settlementMonth, exception);
      }
    }
    var context = contribution.getStepExecution().getExecutionContext();

    context.putInt("targetStoreCount", storeIds.size());
    context.putInt("createdStoreCount", createdCount);
    context.putInt("skippedStoreCount", skippedCount);
    context.putInt("failedStoreCount", failedCount);

    return RepeatStatus.FINISHED;
  }
}
