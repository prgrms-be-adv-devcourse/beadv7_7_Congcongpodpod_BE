package kr.lastdish.core.settlement.application;

import java.time.YearMonth;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {
  private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");

  private final SettlementBatchService settlementBatchService;

  /** 매월 3일 10시와 12시에 직전 달 정산을 실행한다. */
  @Scheduled(cron = "0 0 10,12 3 * *", zone = "Asia/Seoul")
  public void runMonthlySettlement() {
    YearMonth settlementMonth = YearMonth.now(BUSINESS_ZONE).minusMonths(1);

    log.info("월 정산 스케줄 실행 시작. settlementMonth={}", settlementMonth);

    try {
      var response = settlementBatchService.runMonthlySettlement(settlementMonth);
      log.info("월 정산 스케줄 실행 요청 완료. settlementMonth={}, response={}", settlementMonth, response);
    } catch (RuntimeException exception) {
      log.error("월 정산 스케줄 실행 실패. settlementMonth={}", settlementMonth, exception);
    }
  }
}
