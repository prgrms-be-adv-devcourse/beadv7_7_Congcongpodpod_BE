package kr.lastdish.core.settlement.infrastructure.batch;

import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.settlement.domain.SettlementStatus;
import kr.lastdish.core.settlement.infrastructure.JpaSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class MonthlySettlementTargetReader implements ItemStreamReader<Long> {
  private static final String LAST_SETTLEMENT_ID_KEY = "monthlySettlement.lastSettlementId";
  private static final int PAGE_SIZE = 100;
  private static final Set<SettlementStatus> TARGET_STATUSES =
      Set.of(SettlementStatus.ACCUMULATING, SettlementStatus.FAILED);
  private final JpaSettlementRepository settlementRepository;
  private final Queue<Long> buffer = new ArrayDeque<>();

  @Value("#{jobParameters['settlementMonth']}")
  private String settlementMonthValue;

  private YearMonth settlementMonth;
  private long lastSettlementId;
  private boolean exhausted;

  @Override
  public void open(ExecutionContext executionContext) {
    if (settlementMonthValue == null || settlementMonthValue.isBlank()) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT, "settlementMonth는 필수입니다.");
    }

    try {
      settlementMonth = YearMonth.parse(settlementMonthValue);
    } catch (RuntimeException exception) {
      throw new BusinessException(
          CommonErrorCode.INVALID_INPUT, "settlementMonth 형식은 yyyy-MM이어야 합니다.");
    }

    lastSettlementId = executionContext.getLong(LAST_SETTLEMENT_ID_KEY, 0L);
  }

  @Override
  public Long read() {
    if (buffer.isEmpty() && !exhausted) {
      loadNextPage();
    }

    Long settlementId = buffer.poll();
    if (settlementId != null) {
      lastSettlementId = settlementId;
    }

    return settlementId;
  }

  @Override
  public void update(ExecutionContext executionContext) {
    executionContext.putLong(LAST_SETTLEMENT_ID_KEY, lastSettlementId);
  }

  @Override
  public void close() {
    buffer.clear();
  }

  private void loadNextPage() {
    List<Long> settlementIds =
        settlementRepository.findTargetSettlementIdsAfter(
            settlementMonth, TARGET_STATUSES, lastSettlementId, PageRequest.of(0, PAGE_SIZE));
    log.info(
        "reader loadNextPage 이번 배치의 정산 대상 : "
            + settlementIds
            + ", 정산 대상 크기"
            + settlementIds.size());

    buffer.addAll(settlementIds);
    exhausted = settlementIds.isEmpty();
  }
}
