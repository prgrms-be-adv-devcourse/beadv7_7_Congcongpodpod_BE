package kr.lastdish.core.settlement.application.batch;

import kr.lastdish.core.settlement.application.SettlementEventService;
import kr.lastdish.core.settlement.application.dto.SettlementProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlySettlementItemProcessor
    implements ItemProcessor<Long, SettlementProcessResult> {
  private final SettlementEventService settlementEventService;

  @Override
  public SettlementProcessResult process(Long settlementId) {
    return settlementEventService.processMonthlySettlement(settlementId);
  }
}
