package kr.lastdish.core.settlement.application.batch;

import java.util.List;
import kr.lastdish.common.api.exception.BusinessException;
import kr.lastdish.common.api.exception.CommonErrorCode;
import kr.lastdish.core.settlement.application.dto.OrderSettlementAmount;
import kr.lastdish.core.settlement.domain.Settlement;
import kr.lastdish.core.settlement.domain.SettlementDetail;
import kr.lastdish.core.settlement.domain.SettlementDetailRepository;
import kr.lastdish.core.settlement.domain.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SettlementTransactionalManager {
  private final SettlementRepository settlementRepository;
  private final SettlementDetailRepository settlementDetailRepository;

  /*
  정산의 상태 변경 처리 및 데이터 변경, 삽입을 처리하는 클래스(DB 접근 처리)

  기존 매장별 트랜잭션 분리에서 실패 시 새로운 트랜잭션으로 failed 상태를 저장하기 위해
  각 상태별 메서드의 트랜잭션 분리
  */

  // 해당 매장 정산 시작 시 Settlement를 Processing 상태로 저장
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Settlement start(Settlement settlement) {
    return settlementRepository.save(settlement);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void complete(Long settlementId, List<OrderSettlementAmount> calculatedOrders) {
    Settlement settlement = findSettlement(settlementId);

    long totalSalesAmount =
        calculatedOrders.stream().mapToLong(OrderSettlementAmount::salesAmount).sum();
    long totalFeeAmount =
        calculatedOrders.stream().mapToLong(OrderSettlementAmount::feeAmount).sum();
    long totalSettlementAmount =
        calculatedOrders.stream().mapToLong(OrderSettlementAmount::settlementAmount).sum();

    settlement.updateCalculation(
        calculatedOrders.size(), totalSalesAmount, totalFeeAmount, totalSettlementAmount);

    List<SettlementDetail> settlementDetailList =
        calculatedOrders.stream()
            .map(
                calculated ->
                    new SettlementDetail(
                        settlement.getId(),
                        calculated.order().orderId(),
                        calculated.salesAmount(),
                        calculated.feeAmount(),
                        calculated.feeRate(),
                        calculated.settlementAmount(),
                        calculated.order().orderCompletedAt()))
            .toList();

    settlementDetailRepository.bulkInsert(settlementDetailList);
    settlement.complete();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void fail(Long settlementId, String failureReason) {
    Settlement settlement = findSettlement(settlementId);
    settlement.fail(nomalizeFailureReason(failureReason));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Settlement restart(Long settlementId) {
    Settlement settlement = findSettlement(settlementId);
    settlement.restart();

    return settlement;
  }

  private Settlement findSettlement(Long settlementId) {
    return settlementRepository
        .findById(settlementId)
        .orElseThrow(
            () -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "정산 정보를 찾을 수 없습니다."));
  }

  private String nomalizeFailureReason(String failureReason) {
    String reason = failureReason == null ? "알 수 없는 정산 처리 오류" : failureReason;

    return reason.substring(0, Math.min(reason.length(), 300));
  }
}
