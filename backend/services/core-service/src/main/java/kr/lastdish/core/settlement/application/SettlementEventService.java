package kr.lastdish.core.settlement.application;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import kr.lastdish.core.settlement.application.batch.SettlementTransactionalManager;
import kr.lastdish.core.settlement.application.dto.SettlementAccountData;
import kr.lastdish.core.settlement.application.dto.SettlementProcessResult;
import kr.lastdish.core.settlement.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementEventService {
  private final SettlementRepository settlementRepository;
  private final SettlementStoreReader settlementStoreReader;
  private final SettlementTransactionalManager settlementTransactionalManager;

  /*
   * 이벤트 방식의 정산 기능 구현 Service입니다.
   *
   * 미리 정산 내역을 적재하는 방식이며,
   * 정산 내역 적재 시 Settlement 테이블에도 Accumulating 상태의 정산을 저장합니다.
   * 월별 특정 매장 초기 주문 내역 적재 시 저장, 이후에는 기존에 존재하는 Accumulating 상태의 Settlement를 조회해서 적용
   *
   * 정산 시작 시 Accumulating인 매장의 정산 상태를 Processing로 변경
   * 각 매장의 정산 결과를 log로 출력
   *
   * 매장별 트랜잭션 -> 매장의 각각의 DB 변경은 별도의 트랜잭션으로 분리 (유지)
   * -> 정산의 상태 관리를 위함, 현재 구조 상 실패할 일이 적으나,
   *    추후 서비스가 커졌을 때 타 모듈, 서비스와의 통신 오류나 실패 원인별 세부 처리를 위해
   *    정산의 상태를 DB에 적재하기 위함
   *
   * DB 커넥션 및 계산 결과 오류 시 failed 처리 -> 재시도 시 성공 가능
   * 정산 계좌 미등록의 경우 정산하지 않고 로그만 출력하여 관리 -> 계좌 미등록 상태가 유지된다면 재시도 시 실패
   */

  @Transactional(propagation = Propagation.MANDATORY)
  public SettlementProcessResult processMonthlySettlement(Long storeId, YearMonth settlementMonth) {
    Settlement settlement =
        settlementRepository.findByStoreIdAndSettlementMonth(storeId, settlementMonth).orElse(null);

    if (settlement == null) {
      return SettlementProcessResult.skipped(storeId, null, "정산 정보를 찾을 수 없습니다.");
    }

    return processMonthlySettlement(settlement);
  }

  private SettlementProcessResult processMonthlySettlement(Settlement settlement) {
    Long storeId = settlement.getStoreId();

    SettlementStatus initialStatus = settlement.getSettlementStatus();

    if (initialStatus != SettlementStatus.ACCUMULATING
        && initialStatus != SettlementStatus.FAILED) {
      return SettlementProcessResult.skipped(
          storeId, settlement.getId(), "정산 대상 상태가 아닙니다. status=" + initialStatus);
    }

    boolean retry = initialStatus == SettlementStatus.FAILED;

    try {
      if (retry) {
        // Failed 상태 정산인 경우 restart, 매장 정산 계좌 스냅샷 이미 존재
        settlementTransactionalManager.restart(settlement.getId());
      } else {
        // Accumulating 상태 정산의 경우 매장 정산 계좌 미등록 시 skipped
        Optional<SettlementAccountData> account =
            settlementStoreReader.readAccountByStoreId(storeId);

        if (account.isEmpty()) {
          return SettlementProcessResult.skipped(storeId, settlement.getId(), "정산 계좌가 등록되지 않았습니다.");
        }

        settlementTransactionalManager.startAccumulatedSettlement(
            settlement.getId(), account.get());
      }

      settlementTransactionalManager.completeAccumulatedSettlement(settlement.getId());

      // 실패 재처리 분기
      return retry
          ? SettlementProcessResult.retried(storeId, settlement.getId())
          : SettlementProcessResult.created(storeId, settlement.getId());

    } catch (Exception exception) {
      String failureReason = extractFailureReason(exception);

      // 정상 처리와 동일한 Chunk 트랜잭션 안에서 실패 상태를 저장한다.
      // PROCESSING 전환 전 실패했다면 기존 상태를 유지하여 다음 실행에서 다시 시도한다.
      if (settlement.getSettlementStatus() == SettlementStatus.PROCESSING) {
        settlement.fail(normalizeFailureReason(failureReason));
      }

      return SettlementProcessResult.failed(storeId, settlement.getId(), failureReason);
    }
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public SettlementProcessResult processMonthlySettlement(Long settlementId) {
    Settlement settlement = settlementRepository.findById(settlementId).orElse(null);

    if (settlement == null) {
      return SettlementProcessResult.skipped(null, settlementId, "정산 정보를 찾을 수 없습니다.");
    }

    return processMonthlySettlement(settlement);
  }

  @Transactional(readOnly = true)
  public List<Long> findMonthlySettlementTargetStoreIds(YearMonth settlementMonth) {
    return settlementRepository.findTargetStoreIds(
        settlementMonth, Set.of(SettlementStatus.ACCUMULATING, SettlementStatus.FAILED));
  }

  private String extractFailureReason(Exception exception) {
    Throwable cause = exception;

    while (cause.getCause() != null) {
      cause = cause.getCause();
    }

    return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
  }

  private String normalizeFailureReason(String failureReason) {
    String reason = failureReason == null ? "알 수 없는 정산 처리 오류" : failureReason;
    return reason.substring(0, Math.min(reason.length(), 300));
  }
}
